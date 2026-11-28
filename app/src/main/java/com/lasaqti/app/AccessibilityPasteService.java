package com.lasaqti.app;
import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
public class AccessibilityPasteService extends AccessibilityService {
 private static AccessibilityPasteService instance;
 @Override public void onServiceConnected(){super.onServiceConnected();instance=this;}
 @Override public void onAccessibilityEvent(AccessibilityEvent event){}
 @Override public void onInterrupt(){}
 @Override public void onDestroy(){if(instance==this)instance=null;super.onDestroy();}
 private static AccessibilityNodeInfo focusedNode(){
  if(instance==null)return null;
  AccessibilityNodeInfo root=instance.getRootInActiveWindow();if(root==null)return null;
  AccessibilityNodeInfo node=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
  if(node==null)node=root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
  if(node==null)node=root;
  AccessibilityNodeInfo result=AccessibilityNodeInfo.obtain(node);root.recycle();return result;
 }
 public static boolean copyFocused(){AccessibilityNodeInfo n=focusedNode();if(n==null)return false;boolean ok=n.performAction(AccessibilityNodeInfo.ACTION_COPY);n.recycle();return ok;}
 public static boolean pasteFocused(){AccessibilityNodeInfo n=focusedNode();if(n==null)return false;boolean ok=n.performAction(AccessibilityNodeInfo.ACTION_PASTE);n.recycle();return ok;}
 public static int focusedTextLength(){AccessibilityNodeInfo n=focusedNode();if(n==null)return -1;CharSequence t=n.getText();int len=t==null?0:t.length();n.recycle();return len;}
 public static boolean selectAllFocused(){return selectRangeFocused(0,focusedTextLength());}
 public static boolean selectRangeFocused(int start,int end){
  AccessibilityNodeInfo n=focusedNode();if(n==null)return false;CharSequence t=n.getText();int len=t==null?0:t.length();
  if(start<0||end<start||end>len){n.recycle();return false;}
  Bundle b=new Bundle();b.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,start);b.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,end);
  boolean ok=n.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION,b);n.recycle();return ok;
 }
}