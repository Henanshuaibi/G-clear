package cn.g.clear;
import android.content.*;
import android.database.sqlite.*;
import java.io.File;
import java.util.concurrent.*;

/** Executes production SQLite transactions under Android ART, in an isolated test directory. */
public final class DeviceCheck {
 static int count;
 static void ok(boolean b,String n){count++;if(!b)throw new AssertionError(n);}
 public static void main(String[] args) {
  try{run(new File(args[0]));System.exit(0);}catch(Throwable e){e.printStackTrace();System.exit(1);}
 }
 static void run(File dir)throws Exception {
  dir.mkdirs();File db=new File(dir,"clear.db");SQLiteDatabase.deleteDatabase(db);
  Context ctx=new ContextWrapper(null){
   public Context getApplicationContext(){return this;}
   public File getDatabasePath(String name){return new File(dir,name);}
   public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory f){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),f);}
   public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory f,android.database.DatabaseErrorHandler e){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(),f,e);}
  };
  Store s=Store.get(ctx);ok(s.sum(false)[0]==0,"fresh database");
  s.record("app.a","skip");s.record("app.a","back");s.record("app.b","skip");
  ok(s.sum(false)[0]==2 && s.sum(false)[1]==1,"action types");
  ok(s.sum(true)[0]==2 && s.ranks(true).size()==2,"daily package attribution");
  ok(s.events().get(0)[1].equals("app.b"),"recent ordering");
  s.close();ok(s.sum(false)[1]==1,"close reopen persistence");
  ExecutorService pool=Executors.newFixedThreadPool(4);java.util.List<Future<?>> jobs=new java.util.ArrayList<>();
  for(int i=0;i<4;i++)jobs.add(pool.submit(()->{for(int j=0;j<60;j++)s.record("app.concurrent","skip");}));
  for(Future<?> j:jobs)j.get();pool.shutdown();
  ok(s.sum(false)[0]==242,"concurrent increments are not lost");
  ok(android.database.DatabaseUtils.longForQuery(s.getReadableDatabase(),"SELECT COUNT(*) FROM events",null)==200,"bounded events");
  try{s.record("app.invalid","other");throw new AssertionError("bad type accepted");}catch(IllegalArgumentException expected){}
  ok(s.sum(false)[0]==242,"invalid type does not change totals");
  s.getWritableDatabase().execSQL("INSERT INTO daily VALUES('2000-01-01','app.old',1,1)");s.record("app.b","back");
  ok(android.database.DatabaseUtils.longForQuery(s.getReadableDatabase(),"SELECT COUNT(*) FROM daily WHERE day='2000-01-01'",null)==0,"daily retention");
  s.clear();ok(s.sum(false)[0]==0 && s.sum(true)[1]==0 && s.events().isEmpty() && s.ranks(false).isEmpty(),"atomic clear");s.close();
  System.out.println("PASS: "+count+" Android SQLite persistence, concurrency, retention and clear checks");
 }
}
