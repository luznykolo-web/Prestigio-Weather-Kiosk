package pl.luznykolo.prestigioweather;

import android.app.Activity;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.Security;
import org.conscrypt.Conscrypt;

public class MainActivity extends Activity {
 LinearLayout root, hours, days; TextView status,temp,desc,feels,humidity,wind,sunrise,sunset,clock,date;
 ImageView currentIcon; Handler h=new Handler();
 final String URLS="https://api.open-meteo.com/v1/forecast?latitude=51.1136&longitude=20.8716&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m&hourly=temperature_2m,precipitation_probability,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset&timezone=Europe%2FWarsaw&forecast_days=6";
 final Runnable ticker=new Runnable(){public void run(){updateClock();h.postDelayed(this,1000);}};
 final Runnable refresh=new Runnable(){public void run(){fetch();h.postDelayed(this,15*60*1000);}};

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  try {
    // Bundled modern TLS provider. This bypasses the obsolete TLS engine in Android 5.1.
    Security.insertProviderAt(Conscrypt.newProvider(), 1);
  } catch (Throwable ignored) {}
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  hide();build();ticker.run();refresh.run();
 }
 TextView tv(String s,int sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(sp);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
 void build(){
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,10,20,6);root.setBackgroundColor(Color.rgb(5,42,76));setContentView(root);
  LinearLayout top=row();root.addView(top,new LinearLayout.LayoutParams(-1,300));
  LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);top.addView(left,new LinearLayout.LayoutParams(0,-1,1.25f));
  clock=tv("--:--",110);clock.setTypeface(null,Typeface.BOLD);left.addView(clock,new LinearLayout.LayoutParams(-1,150));
  date=tv("",22);date.setTypeface(null,Typeface.BOLD);left.addView(date,new LinearLayout.LayoutParams(-1,38));
  LinearLayout cur=row();left.addView(cur,new LinearLayout.LayoutParams(-1,105));
  currentIcon=new ImageView(this);currentIcon.setImageResource(R.drawable.ic_cloud);currentIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);cur.addView(currentIcon,new LinearLayout.LayoutParams(150,-1));
  LinearLayout ct=new LinearLayout(this);ct.setOrientation(LinearLayout.VERTICAL);cur.addView(ct,new LinearLayout.LayoutParams(0,-1,1));
  temp=tv("--°C",54);temp.setTypeface(null,Typeface.BOLD);ct.addView(temp,new LinearLayout.LayoutParams(-1,65));desc=tv("Oczekiwanie na dane…",20);desc.setTypeface(null,Typeface.BOLD);ct.addView(desc);

  LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);right.setPadding(28,12,0,0);top.addView(right,new LinearLayout.LayoutParams(0,-1,1));
  TextView city=tv("SKARŻYSKO-KAMIENNA",18);city.setGravity(Gravity.RIGHT);city.setTypeface(null,Typeface.BOLD);right.addView(city,new LinearLayout.LayoutParams(-1,60));
  feels=detail(right,"Odczuwalna: --°C");humidity=detail(right,"Wilgotność: --%");wind=detail(right,"Wiatr: -- km/h");sunrise=detail(right,"Wschód słońca: --:--");sunset=detail(right,"Zachód słońca: --:--");

  LinearLayout panels=row();root.addView(panels,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout hp=panel("Prognoza godzinowa",panels,1.55f);hours=row();hp.addView(hours,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout dp=panel("Prognoza na 5 dni",panels,1f);days=row();dp.addView(days,new LinearLayout.LayoutParams(-1,0,1));
  status=tv("Łączenie…",11);root.addView(status,new LinearLayout.LayoutParams(-1,22));
 }
 TextView detail(LinearLayout p,String s){TextView v=tv(s,19);p.addView(v,new LinearLayout.LayoutParams(-1,43));return v;}
 LinearLayout panel(String title,LinearLayout parent,float weight){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(10,5,10,4);parent.addView(p,new LinearLayout.LayoutParams(0,-1,weight));TextView t=tv(title,19);t.setTypeface(null,Typeface.BOLD);p.addView(t,new LinearLayout.LayoutParams(-1,35));return p;}
 void updateClock(){Date n=new Date();clock.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(n));date.setText(new SimpleDateFormat("EEEE, d MMMM yyyy",new Locale("pl","PL")).format(n));}
 void fetch(){status.setText("Łączenie…");new Thread(new Runnable(){public void run(){try{String j=get(URLS);getPreferences(0).edit().putString("cache",j).apply();show(j,true);}catch(final Exception e){final String c=getPreferences(0).getString("cache",null);if(c!=null)show(c,false);else runOnUiThread(new Runnable(){public void run(){String m=e.getClass().getSimpleName();status.setText("Błąd połączenia: "+m);}});}}}).start();}
 String get(String u)throws Exception{
  SSLContext sc=SSLContext.getInstance("TLS");sc.init(null,null,null);
  HttpsURLConnection c=(HttpsURLConnection)new URL(u).openConnection();
  c.setSSLSocketFactory(sc.getSocketFactory());
  c.setConnectTimeout(20000);c.setReadTimeout(20000);
  c.setRequestProperty("Accept","application/json");
  c.setRequestProperty("User-Agent","PrestigioWeather/7");
  InputStream in=c.getInputStream();BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"));StringBuilder b=new StringBuilder();String x;while((x=r.readLine())!=null)b.append(x);r.close();return b.toString();
 }
 void show(final String raw,final boolean online){runOnUiThread(new Runnable(){public void run(){try{render(new JSONObject(raw));status.setText(online?"Dane pobrane: "+new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date()):"Offline — ostatnie zapisane dane");}catch(Exception e){status.setText("Błąd danych pogodowych");}}});}
 int icon(int c){if(c==0)return R.drawable.ic_sun;if(c<=2)return R.drawable.ic_partly;if(c==3||c==45||c==48)return R.drawable.ic_cloud;if((c>=51&&c<=67)||(c>=80&&c<=82))return R.drawable.ic_rain;if(c>=71&&c<=77)return R.drawable.ic_snow;return R.drawable.ic_cloud;}
 String text(int c){if(c==0)return"Bezchmurnie";if(c<=2)return"Częściowe zachmurzenie";if(c==3)return"Pochmurno";if(c==45||c==48)return"Mgła";if(c>=51&&c<=57)return"Mżawka";if(c>=61&&c<=67)return"Deszcz";if(c>=71&&c<=77)return"Śnieg";if(c>=80&&c<=82)return"Przelotny deszcz";if(c>=95)return"Burza";return"Pogoda";}
 int rnd(double d){return(int)Math.round(d);} String hm(String s){return s.length()>=16?s.substring(11,16):"--:--";}
 void render(JSONObject j)throws Exception{
  JSONObject c=j.getJSONObject("current");JSONObject d=j.getJSONObject("daily");JSONObject ho=j.getJSONObject("hourly");int code=c.getInt("weather_code");
  currentIcon.setImageResource(icon(code));temp.setText(rnd(c.getDouble("temperature_2m"))+"°C");desc.setText(text(code));feels.setText("Odczuwalna: "+rnd(c.getDouble("apparent_temperature"))+"°C");humidity.setText("Wilgotność: "+rnd(c.getDouble("relative_humidity_2m"))+"%");wind.setText("Wiatr: "+rnd(c.getDouble("wind_speed_10m"))+" km/h");
  sunrise.setText("Wschód słońca: "+hm(d.getJSONArray("sunrise").getString(0)));sunset.setText("Zachód słońca: "+hm(d.getJSONArray("sunset").getString(0)));
  hours.removeAllViews();JSONArray ht=ho.getJSONArray("time"),hT=ho.getJSONArray("temperature_2m"),hc=ho.getJSONArray("weather_code"),pr=ho.getJSONArray("precipitation_probability");String now=new SimpleDateFormat("yyyy-MM-dd'T'HH:00").format(new Date());int start=0;for(int i=0;i<ht.length();i++)if(ht.getString(i).compareTo(now)>=0){start=i;break;}
  for(int k=0;k<6&&start+k<ht.length();k++)addForecast(hours,hm(ht.getString(start+k)),icon(hc.getInt(start+k)),rnd(hT.getDouble(start+k))+"°C",pr.optInt(start+k,0)+"%");
  days.removeAllViews();JSONArray dt=d.getJSONArray("time"),mx=d.getJSONArray("temperature_2m_max"),mn=d.getJSONArray("temperature_2m_min"),dc=d.getJSONArray("weather_code"),pp=d.getJSONArray("precipitation_probability_max");
  SimpleDateFormat in=new SimpleDateFormat("yyyy-MM-dd",Locale.US), dn=new SimpleDateFormat("EEE",new Locale("pl","PL"));
  for(int q=1;q<=5&&q<dt.length();q++)addForecast(days,dn.format(in.parse(dt.getString(q))),icon(dc.getInt(q)),rnd(mx.getDouble(q))+"° / "+rnd(mn.getDouble(q))+"°",pp.optInt(q,0)+"%");
 }
 void addForecast(LinearLayout p,String a,int res,String b,String rain){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);p.addView(x,new LinearLayout.LayoutParams(0,-1,1));TextView t=tv(a,14);t.setGravity(Gravity.CENTER);x.addView(t,new LinearLayout.LayoutParams(-1,30));ImageView im=new ImageView(this);im.setImageResource(res);im.setScaleType(ImageView.ScaleType.CENTER_INSIDE);x.addView(im,new LinearLayout.LayoutParams(-1,55));TextView v=tv(b,17);v.setGravity(Gravity.CENTER);v.setTypeface(null,Typeface.BOLD);x.addView(v,new LinearLayout.LayoutParams(-1,32));TextView rr=tv("Opady "+rain,12);rr.setGravity(Gravity.CENTER);x.addView(rr);}
 void hide(){getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
 @Override public void onWindowFocusChanged(boolean f){super.onWindowFocusChanged(f);if(f)hide();}
 @Override public void onBackPressed(){}
}
