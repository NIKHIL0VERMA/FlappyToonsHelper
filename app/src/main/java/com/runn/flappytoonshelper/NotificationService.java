package com.runn.flappytoonshelper;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

public class NotificationService extends FirebaseMessagingService {

    String TAG = "Helper";
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        //getting the title and the body
        String title = Objects.requireNonNull(remoteMessage.getNotification()).getTitle();
        String body = remoteMessage.getNotification().getBody();

        Log.d(TAG, "Got msg with Title: " + title + "\nBody: " + body);
        //then here we can use the title and body to build a notification
        CustomNotificationManager.getInstance(this).displayNotification(title, body);

    }
}
