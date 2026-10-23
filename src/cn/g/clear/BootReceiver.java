package cn.g.clear;
import android.content.*;

/** The system binds enabled accessibility services after unlock; never start one as an ordinary service. */
public final class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context c, Intent i) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction()))
            c.getSharedPreferences("clear",0).edit().putLong("last_boot",System.currentTimeMillis()).apply();
    }
}
