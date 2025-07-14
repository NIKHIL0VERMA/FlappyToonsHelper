package com.runn.flappytoonshelper;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.godotengine.godot.Godot;

/**
 * Created by Nikhil Verma.
 * RUNN is owner of the com.runn.flappytoons under Project Flappy Toons.
 * Copyright (c) 2020 RUNN.
 * Don't use the project or it's code without any legal permission.
 * For getting permission to use any part of code.
 * You may contact on nikhil2003verma@gmail.com
 **/

public class CustomNotificationManager {
    private final Context mCtx;
    @SuppressLint("StaticFieldLeak")
    private static CustomNotificationManager mInstance;

    private CustomNotificationManager(Context context) {
        mCtx = context;
    }

    public static synchronized CustomNotificationManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new CustomNotificationManager(context);
        }
        return mInstance;
    }

    public void displayNotification(String title, String body) {
        Intent intent = new Intent(mCtx, Godot.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getActivity(mCtx, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationManager mNotificationManager = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel("com.runn.flappytoons.notification", "News and Updates", NotificationManager.IMPORTANCE_DEFAULT);
            mChannel.setLightColor(Color.RED);
            mChannel.enableLights(true);
            mChannel.setDescription("Used for news and update");

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            // Used to create custom notification sound from raw file
            /*mChannel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+ "://" +
                            mCtx.getPackageName() +
                            "/" +
                            R.raw.sound_score)
                    , audioAttributes);*/
            mChannel.setSound(soundUri, audioAttributes);
            mChannel.setVibrationPattern( new long []{ 100 , 200 , 300 , 400 , 500 , 400 , 300 , 200 , 100 }) ;
            mChannel.enableVibration(true);

            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel( mChannel );
            }
        }
        NotificationCompat.Builder status = new NotificationCompat.Builder(mCtx,"com.runn.flappytoons.notification");
        status.setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ico)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(Color.RED)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_VIBRATE )
                /*.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+ "://" +
                        mCtx.getPackageName() +
                        "/" +
                        R.raw.sound_score))*/
                .setSound(soundUri)
                .setContentIntent(pendingIntent);
        assert mNotificationManager != null;
        mNotificationManager.notify(0 ,
                status.build()) ;
    }
}