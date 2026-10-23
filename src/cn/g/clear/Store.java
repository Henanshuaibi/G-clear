package cn.g.clear;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class Store extends SQLiteOpenHelper {
    private static Store instance;
    public static synchronized Store get(Context c) { if(instance==null) instance=new Store(c.getApplicationContext()); return instance; }
    private Store(Context c) { super(c,"clear.db",null,1); }
    public void onCreate(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE totals(pkg TEXT PRIMARY KEY,skip INTEGER NOT NULL DEFAULT 0,back INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE TABLE daily(day TEXT,pkg TEXT,skip INTEGER NOT NULL DEFAULT 0,back INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(day,pkg))");
        d.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT,at INTEGER,pkg TEXT,kind TEXT)");
    }
    public void onUpgrade(SQLiteDatabase d,int old,int next) { throw new IllegalStateException("Unsupported schema"); }
    public static String day(long now) { return new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date(now)); }
    public synchronized void record(String pkg, String kind) {
        if(!kind.equals("skip") && !kind.equals("back")) throw new IllegalArgumentException();
        SQLiteDatabase d=getWritableDatabase(); long now=System.currentTimeMillis(); String day=day(now);
        d.beginTransaction();
        try {
            d.execSQL("INSERT OR IGNORE INTO totals(pkg) VALUES(?)",new Object[]{pkg});
            d.execSQL("UPDATE totals SET "+kind+"="+kind+"+1 WHERE pkg=?",new Object[]{pkg});
            d.execSQL("INSERT OR IGNORE INTO daily(day,pkg) VALUES(?,?)",new Object[]{day,pkg});
            d.execSQL("UPDATE daily SET "+kind+"="+kind+"+1 WHERE day=? AND pkg=?",new Object[]{day,pkg});
            d.execSQL("INSERT INTO events(at,pkg,kind) VALUES(?,?,?)",new Object[]{now,pkg,kind});
            d.execSQL("DELETE FROM events WHERE id NOT IN (SELECT id FROM events ORDER BY id DESC LIMIT 200)");
            d.execSQL("DELETE FROM daily WHERE day < ?",new Object[]{day(now-90L*86400000)});
            d.setTransactionSuccessful();
        } finally { d.endTransaction(); }
    }
    public synchronized long[] sum(boolean today) {
        try(Cursor c=getReadableDatabase().rawQuery(today?"SELECT COALESCE(SUM(skip),0),COALESCE(SUM(back),0) FROM daily WHERE day=?":"SELECT COALESCE(SUM(skip),0),COALESCE(SUM(back),0) FROM totals",today?new String[]{day(System.currentTimeMillis())}:null)) {
            c.moveToFirst(); return new long[]{c.getLong(0),c.getLong(1)};
        }
    }
    public synchronized List<String[]> ranks(boolean today) {
        List<String[]> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery(today?"SELECT pkg,skip,back FROM daily WHERE day=? ORDER BY (skip+back) DESC":"SELECT pkg,skip,back FROM totals ORDER BY (skip+back) DESC",today?new String[]{day(System.currentTimeMillis())}:null)) {
            while(c.moveToNext()) out.add(new String[]{c.getString(0),c.getString(1),c.getString(2)});
        } return out;
    }
    public synchronized List<String[]> events() {
        List<String[]> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT at,pkg,kind FROM events ORDER BY id DESC LIMIT 40",null)) {
            while(c.moveToNext()) out.add(new String[]{c.getString(0),c.getString(1),c.getString(2)});
        } return out;
    }
    public synchronized void clear() {
        SQLiteDatabase d=getWritableDatabase(); d.beginTransaction();
        try {d.delete("totals",null,null);d.delete("daily",null,null);d.delete("events",null,null);d.setTransactionSuccessful();} finally{d.endTransaction();}
    }
}
