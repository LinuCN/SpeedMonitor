package com.example.speedmonitor.Utils;

import android.Manifest;
import android.content.pm.PackageManager;

import com.example.speedmonitor.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class SMFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Handle the message from Firebase
        if (remoteMessage.getData().size() > 0) {
            String message = remoteMessage.getData().get("message");
            String carId = remoteMessage.getData().get("carId");
            sendSpeedAlertNotification(message, carId);
        }
    }

    private void sendSpeedAlertNotification(String message, String carId) {
        // Code to display a local notification to the user (rental company)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                .setContentTitle("Speed Limit Exceeded")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
              return;
        }
        notificationManager.notify(0, builder.build());
    }
}
