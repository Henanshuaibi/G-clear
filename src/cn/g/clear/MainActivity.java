package cn.g.clear;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final int INK=0xff142a23, MUTED=0xff64726a, GREEN=0xff19795f;
    private LinearLayout body,nav; private TextView status; private int tab=0;
    private SharedPreferences prefs; private boolean today=true,loaded=false;
    private final List<App> apps=new ArrayList<>();
    private final ExecutorService loader=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String search="";
    private final Runnable filter=()->renderApps();
    private static final class App {String pkg,label;App(String p,String l){pkg=p;label=l;}}
    public void onCreate(Bundle state) {
        super.onCreate(state);prefs=getSharedPreferences("clear",0);
        if(state!=null)tab=state.getInt("tab",0);
        LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(20),0,dp(20),0);
        page.setBackgroundColor(0xfff5f6f3); setContentView(page);
        page.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(20),insets.getSystemWindowInsetTop(),dp(20),insets.getSystemWindowInsetBottom());return insets;});
        TextView title=text("G clear",30,INK);title.setTypeface(null,Typeface.BOLD);title.setPadding(0,dp(20),0,dp(4));page.addView(title);
        TextView caption=text("少一点打扰，多一点清净。",14,MUTED);page.addView(caption);
        nav=new LinearLayout(this);nav.setPadding(0,dp(18),0,dp(10));page.addView(nav);
        String[] names={"概览","应用","统计","设置"};for(int i=0;i<names.length;i++){final int id=i;TextView b=text(names[i],15,INK);b.setGravity(Gravity.CENTER);b.setPadding(0,dp(10),0,dp(10));b.setOnClickListener(v->{tab=id;((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);show();});nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));}
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);page.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        body=new LinearLayout(this);body.setOrientation(1);body.setPadding(0,dp(6),0,dp(24));sc.addView(body);
        show();loadApps();
    }
    protected void onResume(){super.onResume();if(body!=null){if(tab==1)refreshStatus();else show();}}
    protected void onSaveInstanceState(Bundle b){b.putInt("tab",tab);super.onSaveInstanceState(b);}
    protected void onDestroy(){handler.removeCallbacksAndMessages(null);loader.shutdownNow();super.onDestroy();}
    private int dp(float x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
    private TextView text(String t,int size,int color){TextView v=new TextView(this);v.setText(t);v.setTextSize(size);v.setTextColor(color);return v;}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout card(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);v.setPadding(dp(16),dp(16),dp(16),dp(16));v.setBackground(bg(Color.WHITE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);body.addView(v,p);return v;}
    private void paragraph(LinearLayout c,String s){TextView t=text(s,14,MUTED);t.setLineSpacing(dp(4),1);t.setPadding(0,dp(8),0,dp(5));c.addView(t);}
    private void heading(LinearLayout c,String s){TextView t=text(s,18,INK);t.setTypeface(null,Typeface.BOLD);c.addView(t);}
    private void button(LinearLayout c,String title,Runnable action){TextView b=text(title,15,GREEN);b.setPadding(0,dp(14),0,dp(10));b.setOnClickListener(v->action.run());c.addView(b);}
    private void show(){
        handler.removeCallbacks(filter);status=null;body.removeAllViews();
        for(int i=0;i<nav.getChildCount();i++){TextView t=(TextView)nav.getChildAt(i);t.setBackground(bg(i==tab?INK:0x00000000,12));t.setTextColor(i==tab?Color.WHITE:INK);}
        if(tab==0)overview();else if(tab==1)appPage();else if(tab==2)stats();else settings();
    }
    private void refreshStatus(){if(status!=null)status.setText(ClearService.connected?(prefs.getBoolean("master",true)?"服务已连接 · 正在防护所选应用":"服务已连接 · 已暂停"):ClearService.enabled(this)?"权限已开启 · 等待系统连接":"尚未开启无障碍服务");}
    private void overview(){
        LinearLayout c=card();status=text("",19,INK);c.addView(status);refreshStatus();
        paragraph(c,"先选择需要防护的 APP，再手动开启无障碍。仅识别前台最初 12 秒内、右上区域的标准跳过按钮。");
        button(c,"开启 / 检查无障碍权限",()->disclosure());
        Switch master=new Switch(this);master.setText("启用防护");master.setChecked(prefs.getBoolean("master",true));master.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("master",on).apply();refreshStatus();});c.addView(master);
        long[] a=Store.get(this).sum(true),all=Store.get(this).sum(false);
        c=card();heading(c,"今天已处理  "+(a[0]+a[1])+" 次");paragraph(c,"跳过操作 "+a[0]+"    跳转返回 "+a[1]+"\n累计处理 "+(all[0]+all[1])+" 次");
        button(c,"查看各 APP 统计",()->{tab=2;show();});
        c=card();heading(c,"从你常用的 APP 开始");paragraph(c,"所有应用默认关闭。跳转防护为实验功能，需逐个开启；正常外链也可能被返回，遇到误判请关闭该 APP 的跳转防护。");
        button(c,"选择防护应用",()->{tab=1;show();});
        c=card();heading(c,"轻量运行，数据留在本机");paragraph(c,"无网络权限、无广告 SDK，不截屏，不记录聊天或页面文字。统计只记录 APP 包名、时间和操作类型。系统接受点击或返回后才计数，不代表广告一定消失。");
    }
    private void loadApps(){
        loader.execute(()->{
            Map<String,App> found=new HashMap<>();Set<String> homes=new HashSet<>();
            for(ResolveInfo r:getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0))homes.add(r.activityInfo.packageName);
            for(ResolveInfo r:getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)) {
                String p=r.activityInfo.packageName;
                if(!p.equals(getPackageName()) && !ClearService.protectedName(p) && !homes.contains(p))found.put(p,new App(p,r.loadLabel(getPackageManager()).toString()));
            }
            List<App> result=new ArrayList<>(found.values());result.sort((a,b)->java.text.Collator.getInstance(Locale.CHINA).compare(a.label,b.label));
            handler.post(()->{if(isDestroyed())return;apps.clear();apps.addAll(result);loaded=true;if(tab==1)renderApps();});
        });
    }
    private LinearLayout appRows;
    private void appPage(){
        LinearLayout c=card();heading(c,"按 APP 选择");paragraph(c,"仅列出有启动入口的 APP；系统设置、桌面不参与防护。开屏跳过需先开启；跳转防护仅在同时识别“跳过”和“广告”标记后短时生效。");
        EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(15);input.setHint("搜索名称或包名");input.setText(search);c.addView(input);
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){search=s.toString();handler.removeCallbacks(filter);handler.postDelayed(filter,180);}public void afterTextChanged(Editable e){}});
        appRows=new LinearLayout(this);appRows.setOrientation(1);body.addView(appRows);renderApps();
    }
    private void renderApps(){
        if(tab!=1 || appRows==null)return;appRows.removeAllViews();
        if(!loaded){paragraph(appRows,"正在读取应用列表…");return;}
        String q=search.toLowerCase(Locale.ROOT).trim();int count=0;
        for(App app:apps) {
            if(!q.isEmpty() && !(app.label.toLowerCase(Locale.ROOT).contains(q)||app.pkg.toLowerCase(Locale.ROOT).contains(q)))continue;
            count++;
            LinearLayout row=new LinearLayout(this);row.setOrientation(1);row.setPadding(dp(14),dp(10),dp(14),dp(10));row.setBackground(bg(Color.WHITE,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);appRows.addView(row,lp);
            heading(row,app.label);TextView pkg=text(app.pkg,11,MUTED);row.addView(pkg);
            Switch skip=new Switch(this);skip.setText("开屏跳过");skip.setChecked(prefs.getBoolean("skip:"+app.pkg,false));row.addView(skip);
            Switch guard=new Switch(this);guard.setText("跳转防护（实验）");guard.setTextSize(14);guard.setChecked(prefs.getBoolean("guard:"+app.pkg,false));guard.setEnabled(skip.isChecked());row.addView(guard);
            skip.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("skip:"+app.pkg,on).apply();guard.setEnabled(on);if(!on){guard.setChecked(false);prefs.edit().putBoolean("guard:"+app.pkg,false).apply();}});
            final boolean[] binding={false};
            guard.setOnCheckedChangeListener((v,on)->{
                if(binding[0])return;
                if(!on){prefs.edit().putBoolean("guard:"+app.pkg,false).apply();return;}
                binding[0]=true;guard.setChecked(false);binding[0]=false;
                sheet("开启跳转防护？","只在开屏短时识别到跳过按钮与广告标记后，对跨 APP 跳转尝试返回。它无法在跳转前拦截，也可能返回你主动打开的链接。", "确认开启",()->{
                    prefs.edit().putBoolean("guard:"+app.pkg,true).apply();binding[0]=true;guard.setChecked(true);binding[0]=false;
                });
            });
        }
        if(count==0)paragraph(appRows,"没有匹配的应用。");
    }
    private String label(String pkg){for(App a:apps)if(a.pkg.equals(pkg))return a.label;try{return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(PackageManager.NameNotFoundException e){return pkg;}}
    private void stats(){
        LinearLayout c=card();heading(c,"处理统计");paragraph(c,"统计的是系统接受的操作次数；未识别、操作失败不计数。同一次进入前台最多发送一次跳过操作，返回归属于广告来源 APP。日期按手机当前时区记录。");button(c,today?"今天 ▾  · 点击切换到累计":"累计 ▾  · 点击切换到今天",()->{today=!today;show();});
        List<String[]> ranks=Store.get(this).ranks(today);
        if(ranks.isEmpty())paragraph(c,"暂无记录。开启防护后，到所选 APP 实际试用。");
        for(String[] r:ranks){LinearLayout row=card();heading(row,label(r[0]));paragraph(row,"跳过 "+r[1]+" 次    跳转返回 "+r[2]+" 次\n"+r[0]);}
        c=card();heading(c,"最近记录");
        SimpleDateFormat f=new SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA);
        for(String[] r:Store.get(this).events())paragraph(c,f.format(new Date(Long.parseLong(r[0])))+"  "+label(r[1])+"\n"+(r[2].equals("skip")?"已发送跳过操作":"已发送返回操作"));
        button(c,"清空统计与记录",()->sheet("清空全部记录？","将清空各 APP 的累计、每日统计与最近记录，无法撤销。应用防护开关保留。","确认清空",()->{Store.get(this).clear();show();}));
    }
    private void settings(){
        LinearLayout c=card();heading(c,"权限与运行状态");status=text("",15,GREEN);c.addView(status);refreshStatus();
        button(c,"无障碍设置",()->disclosure());
        button(c,"G clear 应用详情 / 自启动设置",()->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        button(c,"电池优化设置",()->open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        c=card();heading(c,"开机与清理后台");paragraph(c,"开启无障碍后，由 Android 在开机解锁后重新连接服务，无需保持主界面打开。建议在手机管家允许自启动、把电池策略设为不受限制，并在最近任务中锁定 G clear。各品牌入口不同，应用详情中不一定能直接找到。\n\n清理最近任务通常不等同于强行停止，但厂商可能额外杀进程。强行停止、关闭无障碍、系统限制后无法保证恢复，需重新打开 G clear 并检查服务开关。这里不会自动开启权限或绕过系统限制。");
        long boot=prefs.getLong("last_boot",0);if(boot>0)paragraph(c,"最近收到开机 / 更新广播："+new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(boot))+"。此记录不代表服务已连接。");
        c=card();heading(c,"识别范围");paragraph(c,"支持可访问界面树中的标准文字“跳过”“跳过广告”“Skip ad”及简短倒计时，只处理右上区域的小按钮。非标准按钮、图片 / 视频 / 游戏画布、WebView 未暴露元素、摇一摇广告、APP 内部广告页面，以及已跳出后无法返回的场景可能无效。\n\n不会过滤网络流量，不移除广告素材，也不能阻止其他 APP 发送启动请求。跳转防护属于跳转后返回，不保证恢复原页面；主动外链可能被误判。误判时请先暂停或关闭该 APP 的防护。");
        c=card();heading(c,"权限说明与隐私");paragraph(c,"无障碍允许读取屏幕元素并代你点击或返回，仅在你选择的应用中识别。点击授权前请阅读系统提示；Android 13 及以上侧载安装可能需要在应用详情的菜单中允许受限制设置。\n\n无需 root、悬浮窗、使用情况访问、VPN、文件或联网权限。不保存屏幕文字和截图。数据库仅保存包名、时间、操作类型；最近记录保留 200 条，每日统计保留 90 天，累计统计保留至清空或卸载。\n\nG clear 0.1 · Android 8.0+ · 本机处理");
    }
    private void disclosure(){sheet("开启无障碍前请了解","G clear 将读取前台界面元素，在所选 APP 中点击开屏跳过按钮。单独开启跳转防护后会在广告期间尝试返回跨 APP 跳转。屏幕内容不保存、不上传；可随时在首页暂停或在系统关闭服务。","了解并前往系统设置",()->open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));}
    private void open(Intent i){try{startActivity(i);}catch(ActivityNotFoundException|SecurityException e){Toast.makeText(this,"系统未提供此入口，请手动打开手机设置",Toast.LENGTH_LONG).show();}}
    private void sheet(String title,String message,String confirm,Runnable action){
        Dialog d=new Dialog(this);LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(24),dp(22),dp(24),dp(24));content.setBackground(bg(Color.WHITE,24));heading(content,title);paragraph(content,message);button(content,confirm,()->{d.dismiss();action.run();});button(content,"取消",d::dismiss);ScrollView sc=new ScrollView(this);sc.addView(content);d.setContentView(sc);Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.setLayout(-1,-2);}d.show();if(w!=null)w.setLayout(-1,-2);
    }
}
