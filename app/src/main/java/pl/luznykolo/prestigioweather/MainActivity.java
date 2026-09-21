package pl.luznykolo.prestigioweather;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class MainActivity extends Activity {
    private WebView webView;
    private final Handler handler = new Handler();
    private boolean pageReady = false;

    private static final long REFRESH_MS = 15L * 60L * 1000L;
    private static final String WEATHER_URL =
            "https://api.open-meteo.com/v1/forecast" +
            "?latitude=51.1136&longitude=20.8716" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset" +
            "&timezone=Europe%2FWarsaw&forecast_days=6";

    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            fetchWeather();
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setDefaultTextEncodingName("utf-8");
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                pageReady = true;
                showCachedWeather();
                fetchWeather();
            }
        });
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void fetchWeather() {
        new Thread(new Runnable() {
            @Override public void run() {
                HttpsURLConnection connection = null;
                try {
                    URL url = new URL(WEATHER_URL);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setSSLSocketFactory(new Tls12SocketFactory());
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("User-Agent", "PrestigioWeatherStation/5.0");
                    connection.connect();

                    int code = connection.getResponseCode();
                    if (code < 200 || code >= 300) throw new Exception("HTTP " + code);

                    String json = readAll(connection.getInputStream());
                    new JSONObject(json); // validate JSON
                    getSharedPreferences("weather", MODE_PRIVATE)
                            .edit().putString("last_json", json).apply();

                    deliver(json, true);
                } catch (Exception e) {
                    String cached = getSharedPreferences("weather", MODE_PRIVATE)
                            .getString("last_json", null);
                    if (cached != null) deliver(cached, false);
                    else setOfflineNoData();
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        }).start();
    }

    private String readAll(InputStream input) throws Exception {
        BufferedReader r = new BufferedReader(
                new InputStreamReader(input, Charset.forName("UTF-8")));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) out.append(line);
        r.close();
        return out.toString();
    }

    private void showCachedWeather() {
        String cached = getSharedPreferences("weather", MODE_PRIVATE)
                .getString("last_json", null);
        if (cached != null) deliver(cached, false);
    }

    private void deliver(final String json, final boolean online) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (!pageReady) return;
                String quoted = JSONObject.quote(json);
                webView.evaluateJavascript(
                        "window.receiveWeather(" + quoted + "," + online + ");", null);
            }
        });
    }

    private void setOfflineNoData() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (pageReady) webView.evaluateJavascript("window.weatherFailed();", null);
            }
        });
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override protected void onResume() {
        super.onResume();
        hideSystemUI();
        handler.removeCallbacks(refreshRunnable);
        handler.post(refreshRunnable);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    @Override public void onBackPressed() { }

    // Android 5.0/5.1: force TLS 1.2 for HTTPS sockets.
    private static class Tls12SocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;

        Tls12SocketFactory() throws Exception {
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, null, null);
            delegate = context.getSocketFactory();
        }

        private java.net.Socket enable(java.net.Socket socket) {
            if (socket instanceof SSLSocket) {
                ((SSLSocket) socket).setEnabledProtocols(new String[]{"TLSv1.2"});
            }
            return socket;
        }

        @Override public String[] getDefaultCipherSuites() { return delegate.getDefaultCipherSuites(); }
        @Override public String[] getSupportedCipherSuites() { return delegate.getSupportedCipherSuites(); }
        @Override public java.net.Socket createSocket(java.net.Socket s, String h, int p, boolean a) throws java.io.IOException {
            return enable(delegate.createSocket(s, h, p, a));
        }
        @Override public java.net.Socket createSocket(String h, int p) throws java.io.IOException {
            return enable(delegate.createSocket(h, p));
        }
        @Override public java.net.Socket createSocket(String h, int p, java.net.InetAddress l, int lp) throws java.io.IOException {
            return enable(delegate.createSocket(h, p, l, lp));
        }
        @Override public java.net.Socket createSocket(java.net.InetAddress h, int p) throws java.io.IOException {
            return enable(delegate.createSocket(h, p));
        }
        @Override public java.net.Socket createSocket(java.net.InetAddress h, int p, java.net.InetAddress l, int lp) throws java.io.IOException {
            return enable(delegate.createSocket(h, p, l, lp));
        }
    }
}
