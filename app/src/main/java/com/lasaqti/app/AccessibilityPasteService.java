package com.lasaqti.app;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class AccessibilityPasteService extends AccessibilityService {
    private static AccessibilityPasteService instance;
    private static AccessibilityNodeInfo lastFocused;

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            type == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null && source.isEditable() && source.isEnabled()) {
                synchronized (AccessibilityPasteService.class) {
                    if (lastFocused != null) lastFocused.recycle();
                    lastFocused = AccessibilityNodeInfo.obtain(source);
                }
            } else if (source != null) {
                source.recycle();
            }
        }
    }

    @Override public void onInterrupt() {}

    @Override public void onDestroy() {
        instance = null;
        synchronized (AccessibilityPasteService.class) {
            if (lastFocused != null) lastFocused.recycle();
            lastFocused = null;
        }
        super.onDestroy();
    }

    private static AccessibilityNodeInfo findEditable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable() && node.isEnabled() && node.isFocused()) return AccessibilityNodeInfo.obtain(node);
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findEditable(child);
            if (child != null) child.recycle();
            if (found != null) return found;
        }
        return null;
    }

    private static AccessibilityNodeInfo focusedNode() {
        if (instance == null) return null;
        synchronized (AccessibilityPasteService.class) {
            if (lastFocused != null) {
                AccessibilityNodeInfo copy = AccessibilityNodeInfo.obtain(lastFocused);
                if (copy.refresh() && copy.isEditable() && copy.isEnabled()) return copy;
                copy.recycle();
            }
        }
        AccessibilityNodeInfo root = instance.getRootInActiveWindow();
        if (root == null) return null;
        AccessibilityNodeInfo node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (node == null || !node.isEditable()) {
            if (node != null) node.recycle();
            node = findEditable(root);
        } else {
            AccessibilityNodeInfo copy = AccessibilityNodeInfo.obtain(node);
            node.recycle();
            node = copy;
        }
        root.recycle();
        return node;
    }

    private static boolean clipboardCopy(CharSequence text) {
        if (instance == null || text == null) return false;
        ClipboardManager cm = (ClipboardManager) instance.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return false;
        cm.setPrimaryClip(ClipData.newPlainText("لصقتي", text));
        return true;
    }

    public static boolean copyFocused() {
        AccessibilityNodeInfo n = focusedNode();
        if (n == null) return false;
        boolean ok = n.performAction(AccessibilityNodeInfo.ACTION_COPY);
        if (!ok) {
            CharSequence text = n.getText();
            int a = n.getTextSelectionStart(), b = n.getTextSelectionEnd();
            if (text != null && a >= 0 && b > a && b <= text.length())
                ok = clipboardCopy(text.subSequence(a, b));
        }
        n.recycle();
        return ok;
    }

    public static boolean pasteFocused() {
        AccessibilityNodeInfo n = focusedNode();
        if (n == null || instance == null) { if (n != null) n.recycle(); return false; }
        boolean ok = n.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        if (!ok) {
            ClipboardManager cm = (ClipboardManager) instance.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence insert = cm.getPrimaryClip().getItemAt(0).coerceToText(instance);
                CharSequence old = n.getText();
                int len = old == null ? 0 : old.length();
                int a = n.getTextSelectionStart(), b = n.getTextSelectionEnd();
                if (a < 0 || b < 0) a = b = len;
                a = Math.max(0, Math.min(a, len));
                b = Math.max(a, Math.min(b, len));
                String updated = (old == null ? "" : old.subSequence(0, a)) +
                    (insert == null ? "" : insert) +
                    (old == null ? "" : old.subSequence(b, len));
                Bundle args = new Bundle();
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updated);
                ok = n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            }
        }
        n.recycle();
        return ok;
    }

    public static int focusedTextLength() {
        AccessibilityNodeInfo n = focusedNode();
        if (n == null) return -1;
        CharSequence t = n.getText();
        int len = t == null ? 0 : t.length();
        n.recycle();
        return len;
    }

    public static boolean selectAllFocused() {
        AccessibilityNodeInfo n = focusedNode();
        if (n == null) return false;
        CharSequence t = n.getText();
        int len = t == null ? 0 : t.length();
        boolean ok = n.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs(0, len));
        n.recycle();
        return ok;
    }

    private static Bundle selectionArgs(int start, int end) {
        Bundle b = new Bundle();
        b.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start);
        b.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end);
        return b;
    }

    public static boolean selectRangeFocused(int start, int end) {
        AccessibilityNodeInfo n = focusedNode();
        if (n == null) return false;
        CharSequence t = n.getText();
        int len = t == null ? 0 : t.length();
        boolean ok = start >= 0 && end >= start && end <= len &&
            n.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs(start, end));
        n.recycle();
        return ok;
    }
}