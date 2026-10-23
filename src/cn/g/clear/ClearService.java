package cn.g.clear;

import android.accessibilityservice.*;
import android.app.KeyguardManager;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.*;
import java.util.*;

public final class ClearService extends AccessibilityService {
    public static volatile boolean connected;
    private final Rules.Session session=new Rules.Session();
    private final Set<String> protectedPackages=new HashSet<>();
    private long lastScan=-1000;
    private SharedPreferences prefs;
    public void onServiceConnected() {
        prefs=getSharedPreferences("clear",0);
        protectedPackages.addAll(Arrays.asList(getPackageName(),"android","com.android.systemui","com.android.settings","com.android.permissioncontroller","com.google.android.permissioncontroller"));
        Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for(ResolveInfo r:getPackageManager().queryIntentActivities(home,0)) protectedPackages.add(r.activityInfo.packageName);
        connected=true; session.reset();
    }
    public static boolean enabled(Context c) {
        String list=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        ComponentName target=new ComponentName(c,ClearService.class);
        if(list!=null) for(String flat:list.split(":")) if(target.equals(ComponentName.unflattenFromString(flat))) return true;
        return false;
    }
    public static boolean protectedName(String p) {
        return p.equals("android") || p.equals("com.android.systemui") || p.contains("permissioncontroller") || p.equals("com.android.settings") || p.startsWith("com.android.packageinstaller");
    }
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if(prefs==null) return;
        if(!prefs.getBoolean("master",true) || ((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked()) { session.reset();return; }
        long now=SystemClock.elapsedRealtime();
        if(now-lastScan<220) return;
        lastScan=now;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null) return;
        try {
            if(root.getPackageName()==null) return;
            String pkg=root.getPackageName().toString();
            // An event may describe an old/background app: always use the active root.
            boolean boundary=protectedPackages.contains(pkg) || protectedName(pkg) || isKeyboard(pkg);
            String origin=session.enter(pkg,now,boundary,!boundary);
            if(boundary) {session.clearEvidence();return;}
            if(origin!=null && prefs.getBoolean("skip:"+origin,false) && prefs.getBoolean("guard:"+origin,false)) {
                if(performGlobalAction(GLOBAL_ACTION_BACK)) {
                    session.action(now); Store.get(this).record(origin,"back"); return;
                }
            }
            if(!prefs.getBoolean("skip:"+pkg,false) || !session.scanning(now)) return;
            scan(root,pkg,now);
        } catch(IllegalStateException | SecurityException ignored) {
            session.clearEvidence();
        } finally {root.recycle();}
    }
    private boolean isKeyboard(String pkg) {
        String ime=Settings.Secure.getString(getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);
        ComponentName c=ime==null?null:ComponentName.unflattenFromString(ime);
        return c!=null && pkg.equals(c.getPackageName());
    }
    private void scan(AccessibilityNodeInfo root,String pkg,long now) {
        ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>();
        queue.add(AccessibilityNodeInfo.obtain(root));
        AccessibilityNodeInfo candidate=null;
        boolean ad=false,skipSeen=false;
        Rect screen=new Rect();root.getBoundsInScreen(screen);
        int width=screen.right,height=screen.bottom,visited=0;
        long deadline=SystemClock.uptimeMillis()+18;
        try {
            while(!queue.isEmpty() && visited++<180 && SystemClock.uptimeMillis()<deadline) {
                AccessibilityNodeInfo n=queue.removeFirst();
                try {
                    if(!n.isVisibleToUser() || !n.isEnabled() || n.isPassword()) continue;
                    String text=n.getText()==null?null:n.getText().toString();
                    String desc=n.getContentDescription()==null?null:n.getContentDescription().toString();
                    ad |= Rules.ad(text) || Rules.ad(desc);
                    Rect b=new Rect(); n.getBoundsInScreen(b);
                    if((Rules.skip(text)||Rules.skip(desc)) && Rules.corner(b.left,b.top,b.right,b.bottom,width,height)) {
                        skipSeen=true;
                        if(candidate==null) candidate=clickable(n,width,height);
                    }
                    int count=Math.min(n.getChildCount(),180-visited-queue.size());
                    for(int i=0;i<count;i++) {AccessibilityNodeInfo child=n.getChild(i);if(child!=null)queue.add(child);}
                } finally {n.recycle();}
            }
            if(skipSeen && ad && prefs.getBoolean("guard:"+pkg,false)) session.evidence(pkg,now);
            else session.clearEvidence();
            if(candidate!=null && candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                session.action(now); Store.get(this).record(pkg,"skip");
            }
        } finally {
            if(candidate!=null) candidate.recycle();
            while(!queue.isEmpty())queue.removeFirst().recycle();
        }
    }
    private AccessibilityNodeInfo clickable(AccessibilityNodeInfo n,int width,int height) {
        AccessibilityNodeInfo current=AccessibilityNodeInfo.obtain(n);
        for(int depth=0;depth<3 && current!=null;depth++) {
            Rect b=new Rect();current.getBoundsInScreen(b);
            if(!current.isVisibleToUser() || !current.isEnabled() || !Rules.corner(b.left,b.top,b.right,b.bottom,width,height)) {current.recycle();return null;}
            if(current.isClickable()) return current;
            AccessibilityNodeInfo parent=current.getParent();current.recycle();current=parent;
        }
        if(current!=null)current.recycle();return null;
    }
    public void onInterrupt() {session.reset();}
    public boolean onUnbind(Intent i) {connected=false;session.reset();return super.onUnbind(i);}
    public void onDestroy() {connected=false;session.reset();super.onDestroy();}
}
