package cn.g.clear.fixture;
import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
public final class MainActivity extends Activity {
 public void onCreate(Bundle b){super.onCreate(b);
  boolean negative=getSharedPreferences("test",0).getBoolean("negative",false);
  FrameLayout root=new FrameLayout(this);root.setPadding(0,70,0,60);root.setBackgroundColor(0xfff5f6f3);setContentView(root);
  LinearLayout body=new LinearLayout(this);body.setPadding(36,180,36,36);body.setOrientation(1);root.addView(body);
  TextView title=new TextView(this);title.setTextSize(24);title.setText(negative?"误点检查":"广告");body.addView(title);
  TextView message=new TextView(this);message.setTextSize(17);message.setText(negative?"右上方是“跳过教程”，底部是“跳过”。G clear 都不应点击。":"这是本机模拟开屏，无联网与真实广告。\n\n在 G clear 的应用页开启本 APP 的开屏跳过，再回到桌面重新打开本 APP。\n\n右上按钮被点击后，此处会显示回执；统计应只增加一次。");body.addView(message);
  Button skip=new Button(this);skip.setText(negative?"跳过教程":"跳过 3s");
  FrameLayout.LayoutParams pos=new FrameLayout.LayoutParams(320,130,Gravity.TOP|Gravity.RIGHT);pos.topMargin=80;pos.rightMargin=15;root.addView(skip,pos);
  skip.setOnClickListener(v->{skip.setVisibility(View.GONE);title.setText(negative?"检测到误点！":"已收到跳过点击");message.setText("请回到 G clear 核对本 APP 的计数。此回执也可能来自你的手动点击，测试时请勿触摸右上按钮。");});
  Button mode=new Button(this);mode.setText(negative?"切换为开屏测试":"切换为误点检查");body.addView(mode);mode.setOnClickListener(v->{getSharedPreferences("test",0).edit().putBoolean("negative",!negative).apply();finish();});
  TextView help=new TextView(this);help.setText("切换后会退出，请回桌面重新打开。未开启服务或未选择测试 APP 时不会自动点击。");body.addView(help);
  if(negative){Button bottom=new Button(this);bottom.setText("跳过");FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(320,130,Gravity.BOTTOM|Gravity.RIGHT);p.bottomMargin=60;root.addView(bottom,p);bottom.setOnClickListener(v->title.setText("检测到底部误点！"));}
 }
}
