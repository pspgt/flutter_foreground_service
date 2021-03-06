package it.zeruel.flutter_foreground_service;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterNativeView;



public class ForegroundService extends Service  {

    private static final int ONGOING_NOTIFICATION_ID = 1;
    private String CHANNEL_ID = "flutter_foreground_service_channel_id";
    private static ThreadRunner t;
    private static PluginRegistry.PluginRegistrantCallback pluginRegistrantCallback;
    public static MethodChannel backgroundChannel;

    public static void setPluginRegistrant(PluginRegistry.PluginRegistrantCallback cb) {
        pluginRegistrantCallback = cb;
    }

    public static void setMethodChannel(MethodChannel background) {
        backgroundChannel = background;
    }

    public static boolean setBackgroundFlutterView(FlutterNativeView view) {
        return  t.setBackgroundFlutterView(view);
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String name = "flutter_foreground_service";
            String description = "description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals("start")) {
            PackageManager pm = getApplicationContext().getPackageManager();
            Intent notificationIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Bundle bundle = intent.getExtras();
            long handle = bundle.getLong("handle");

            Notification notification =
                    new NotificationCompat.Builder(this,CHANNEL_ID)
                            .setOngoing(true)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentTitle(bundle.getString("title"))
                            .setContentText(bundle.getString("text"))
                            .setSubText(bundle.getString("subText"))
                            .setTicker(bundle.getString("ticker"))
                            .setContentIntent(pendingIntent)
                            .build();
            startForeground(ONGOING_NOTIFICATION_ID, notification);
            final Map<String, Long> arg = new HashMap<String,Long>();
            arg.put("handle",handle);
            arg.put("init", bundle.getLong("init"));
            int timeout = bundle.getInt("timeout");

            Log.d("fs","avvio thread");

           t = new ThreadRunner(getApplicationContext(),arg,timeout,pluginRegistrantCallback);

        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        t.stop();
        stopSelf();
        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}