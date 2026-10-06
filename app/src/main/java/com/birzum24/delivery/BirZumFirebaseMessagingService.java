package com.birzum24.delivery;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class BirZumFirebaseMessagingService
        extends FirebaseMessagingService {

    private static final String CHANNEL_ID =
            "birzum_orders";

    @Override
    public void onNewToken(String token) {

        super.onNewToken(token);

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences(
                        "birzum_delivery",
                        Context.MODE_PRIVATE
                );

        prefs.edit()
                .putString("fcm_token", token)
                .apply();
    }

    @Override
    public void onMessageReceived(
            RemoteMessage remoteMessage
    ) {

        super.onMessageReceived(remoteMessage);

        createNotificationChannel();

        String title =
                "🚚 BirZum24";

        String body =
                "Sizga yangi buyurtma mavjud.";

        /*
         * Firebase notification qismi.
         */
        if (remoteMessage.getNotification() != null) {

            String notificationTitle =
                    remoteMessage
                            .getNotification()
                            .getTitle();

            String notificationBody =
                    remoteMessage
                            .getNotification()
                            .getBody();

            if (notificationTitle != null
                    && !notificationTitle.trim().isEmpty()) {

                title = notificationTitle;
            }

            if (notificationBody != null
                    && !notificationBody.trim().isEmpty()) {

                body = notificationBody;
            }
        }

        /*
         * Firebase data qismi.
         */
        String type =
                remoteMessage.getData()
                        .get("type");

        String orderId =
                remoteMessage.getData()
                        .get("order_id");

        showNotification(
                title,
                body,
                type,
                orderId
        );
    }

    private void showNotification(
            String title,
            String body,
            String type,
            String orderId
    ) {

        /*
         * Notification bosilganda
         * MainActivity ochiladi.
         */
        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        if (type != null) {

            intent.putExtra(
                    "notification_type",
                    type
            );
        }

        if (orderId != null) {

            intent.putExtra(
                    "order_id",
                    orderId
            );
        }

        int requestCode;

        if (orderId != null
                && !orderId.trim().isEmpty()) {

            requestCode =
                    orderId.hashCode();

        } else {

            requestCode =
                    (int) System.currentTimeMillis();
        }

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        Uri soundUri =
                RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_NOTIFICATION
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                R.drawable.ic_launcher
                        )
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(body)
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )
                        .setCategory(
                                NotificationCompat.CATEGORY_MESSAGE
                        )
                        .setAutoCancel(true)
                        .setContentIntent(
                                pendingIntent
                        )
                        .setSound(soundUri)
                        .setVibrate(
                                new long[]{
                                        0,
                                        400,
                                        200,
                                        400
                                }
                        )
                        .setVisibility(
                                NotificationCompat.VISIBILITY_PUBLIC
                        );

        /*
         * Android 13+ notification permission.
         */
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        }

        NotificationManagerCompat manager =
                NotificationManagerCompat.from(
                        this
                );

        int notificationId;

        if (orderId != null
                && !orderId.trim().isEmpty()) {

            notificationId =
                    orderId.hashCode();

        } else {

            notificationId =
                    (int) System.currentTimeMillis();
        }

        manager.notify(
                notificationId,
                builder.build()
        );
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        if (manager == null) {
            return;
        }

        Uri soundUri =
                RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_NOTIFICATION
                );

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Buyurtmalar",
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.setDescription(
                "BirZum24 yangi buyurtma bildirishnomalari"
        );

        channel.enableVibration(true);

        channel.setSound(
                soundUri,
                null
        );

        manager.createNotificationChannel(
                channel
        );
    }
    }
