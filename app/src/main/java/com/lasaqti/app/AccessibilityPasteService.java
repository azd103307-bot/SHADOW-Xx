package com.lasaqti.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class AccessibilityPasteService extends AccessibilityService {
    private static AccessibilityPasteService instance;
    @Override public void onServiceConnected(){ super.onServiceConnected(); instance=this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}
    @Override public void onDestroy(){ if(instance==this) instance=null; super.onDestroy(); }
    public static boolean copyFocused(){
        if(instance==null) return false;
        AccessibilityNodeInfo root=instance.getRootInActiveWindow(); if(root==null) return false;
        AccessibilityNodeInfo node=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if(node==null) node=root;
        boolean ok=node.performAction(AccessibilityNodeInfo.ACTION_COPY);
        if(node!=root) node.recycle(); root.recycle(); return ok;
    }
    public static boolean pasteFocused(){
        if(instance==null) return false;
        AccessibilityNodeInfo root=instance.getRootInActiveWindow(); if(root==null) return false;
        AccessibilityNodeInfo node=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if(node==null){root.recycle();return false;}
        boolean ok=node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        node.recycle(); root.recycle(); return ok;
    }
}