package com.runn.flappytoonshelper;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Objects;

/**
 * Created by Nikhil Verma.
 * RUNN is owner of the com.runn.flappytoons under Project Flappy Toons.
 * Copyright (c) 2020 RUNN.
 * Don't use the project or it's code without any legal permission.
 * For getting permission to use any part of code.
 * You may contact on nikhil2003verma@gmail.com
 **/

public class NotificationService extends FirebaseMessagingService {

    String TAG = "Helper";
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        Helper.token = token;
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String title = Objects.requireNonNull(remoteMessage.getNotification()).getTitle();
        String body = remoteMessage.getNotification().getBody();

        Log.d(TAG, "Got msg with Title: " + title + "\nBody: " + body);
        CustomNotificationManager.getInstance(this).displayNotification(title, body);

    }
}
