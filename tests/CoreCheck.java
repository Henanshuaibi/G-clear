import cn.g.clear.Rules;
public final class CoreCheck {
 static int count;
 static void ok(boolean b,String name){count++;if(!b)throw new AssertionError(name);}
 public static void main(String[] args){
  for(String s:new String[]{"跳过","跳过广告","跳过 5","跳过(3秒)","5s 跳过","Skip ad","SKIP AD 3s","skip (5)","  跳过  "})ok(Rules.skip(s),s);
  for(String s:new String[]{"跳过教程","不可跳过广告","点击跳过广告领取奖励","skip tutorial","skip adapter","跳过 100","跳过并购买","广告","",null})ok(!Rules.skip(s),"reject "+s);
  ok(Rules.ad("广告"),"ad");ok(!Rules.ad("这不是广告"),"no substring");
  ok(Rules.corner(800,80,980,150,1000,2000),"corner");
  ok(!Rules.corner(0,0,1000,2000,1000,2000),"whole ad parent");
  ok(!Rules.corner(800,900,980,980,1000,2000),"bottom");
  ok(!Rules.corner(100,10,200,50,1000,2000),"top left");
  ok(!Rules.corner(900,10,1100,50,1000,2000),"outside");
  ok(!Rules.corner(0,0,0,0,0,0),"empty");
  Rules.Session s=new Rules.Session();s.reset();s.enter("source",100,false,true);
  ok(s.scanning(101),"new session");ok(s.scanning(12100),"12 second boundary");ok(!s.scanning(12101),"late skip rejected");
  s.evidence("source",200);s.action(200);ok(!s.scanning(250),"no duplicate click");
  ok("source".equals(s.enter("target",300,false,true)),"guard origin");
  ok(s.enter("another",400,false,true)==null,"one back only");
  s.enter("source",500,false,true);s.evidence("source",600);s.enter("home",700,true,false);
  ok(s.enter("target",800,false,true)==null,"home clears evidence");
  s.enter("source",900,false,true);s.evidence("source",1000);ok(s.enter("target",4500,false,true)==null,"expiry");
  s.enter("source",5000,false,true);s.evidence("source",5100);s.clearEvidence();ok(s.enter("target",5200,false,true)==null,"no ad clears evidence");
  s.enter("source",6000,false,true);s.evidence("source",6100);ok(s.enter("target",6200,false,false)==null,"excluded destination");
  s.enter("source",7000,false,true);s.evidence("source",7100);s.reset();ok(s.enter("target",7200,false,true)==null,"pause/lock clears evidence");
  System.out.println("PASS: "+count+" rule, false-positive, cooldown, attribution and guard checks");
 }
}
