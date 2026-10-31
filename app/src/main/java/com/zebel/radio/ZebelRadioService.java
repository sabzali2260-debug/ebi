package com.zebel.radio;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
public final class ZebelRadioService extends Service {
    private static final String CHANNEL="zebel_radio_live";private static final int ID=4107;private static final String START="com.zebel.radio.START";
    public static void start(Context c,String station){Intent i=new Intent(c,ZebelRadioService.class);i.setAction(START);i.putExtra("station",station);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
    public static void stop(Context c){c.stopService(new Intent(c,ZebelRadioService.class));}
    @Override public void onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"پخش زنده ZEBEL RADIO",NotificationManager.IMPORTANCE_LOW);ch.setDescription("وضعیت پخش ایستگاه رادیویی");ch.setShowBadge(false);NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.createNotificationChannel(ch);}}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String station=intent==null?"پخش زنده":intent.getStringExtra("station");
        Intent open=new Intent(this,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int pf=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)pf|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi=PendingIntent.getActivity(this,0,open,pf);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_stat_radio).setColor(Color.rgb(255,173,75)).setContentTitle("ZEBEL RADIO")
         .setContentText((station==null||station.trim().isEmpty()?"در حال پخش":station.trim())+" • LIVE")
         .setCategory(Notification.CATEGORY_TRANSPORT).setVisibility(Notification.VISIBILITY_PUBLIC).setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false).setContentIntent(pi);
        startForeground(ID,b.build());return START_STICKY;
    }
    @Override public void onDestroy(){if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_REMOVE);else stopForeground(true);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
