package pl.luznykolo.weatherkiosk;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String URL = "https://luznykolo-web.github.io/stacja-pogodowa/";
    private final Handler handler = new Handler();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        TextView v = new TextView(this);
        v.setText("Uruchamianie stacji pogodowej…\n\nPrzy pierwszym uruchomieniu wybierz Firefox.");
        v.setTextSize(24); v.setGravity(17); setContentView(v);
        handler.postDelayed(new Runnable(){ public void run(){ openWeather(); }}, 900);
    }

    @Override protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void openWeather() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
            i.setPackage("org.mozilla.firefox");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
            startActivity(i);
        }
    }
}
