package pl.luznykolo.weatherkiosk;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        Intent x = new Intent(c, MainActivity.class);
        x.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(x);
    }
}
