package com.birzum24.delivery;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class BirZumFirebaseMessagingService
        extends FirebaseMessagingService {

    private static final String CHANNEL_ID =
            "birzum_orders_v3";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        getSharedPreferences(
                "birzum24_prefs",
                MODE_PRIVATE
        )
                .edit()
                .putString("fcm_token", token)
                .remove("fcm_synced_token")
                .apply();
    }

    @Override
    public void onMessageReceived(
            @NonNull RemoteMessage remoteMessage
    ) {

        String title = "BirZum24";

        String body = "Yangi buyurtma mavjud";

        String orderId = "";

        /*
         * FCM notification payload
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
                    && !notificationTitle.isEmpty()) {

                title = notificationTitle;
            }

            if (notificationBody != null
                    && !notificationBody.isEmpty()) {

                body = notificationBody;
            }
        }

        /*
         * FCM data payload
         */
        if (!remoteMessage.getData().isEmpty()) {

            String dataTitle =
                    remoteMessage
                            .getData()
                            .get("title");

            String dataBody =
                    remoteMessage
                            .getData()
                            .get("body");

            String dataOrderId =
                    remoteMessage
                            .getData()
                            .get("order_id");

            if (dataTitle != null
                    && !dataTitle.isEmpty()) {

                title = dataTitle;
            }

            if (dataBody != null
                    && !dataBody.isEmpty()) {

                body = dataBody;
            }

            if (dataOrderId != null) {

                orderId = dataOrderId;
            }
        }

        showNotification(
                title,
                body,
                orderId
        );
    }

    private void showNotification(
            String title,
            String body,
            String orderId
    ) {

        NotificationManagerCompat manager =
                NotificationManagerCompat.from(this);

        /*
         * Android 13+
         */
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        }

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        if (orderId != null
                && !orderId.isEmpty()) {

            intent.putExtra(
                    "order_id",
                    orderId
            );
        }

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        orderId == null
                                ? 0
                                : orderId.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
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
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setOngoing(false)
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )
                        .setCategory(
                                NotificationCompat.CATEGORY_MESSAGE
                        )
                        .setVisibility(
                                NotificationCompat.VISIBILITY_PUBLIC
                        )
                        .setDefaults(
                                NotificationCompat.DEFAULT_ALL
                        )
                        .setWhen(
                                System.currentTimeMillis()
                        )
                        .setShowWhen(true);

        int notificationId;

        if (orderId != null
                && !orderId.isEmpty()) {

            try {

                notificationId =
                        Integer.parseInt(orderId);

            } catch (Exception e) {

                notificationId =
                        orderId.hashCode();
            }

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

        /*
         * Eski channel nomini ishlatmaymiz.
         *
         * V3 yangi channel bo'lgani uchun Android
         * uni qaytadan HIGH importance bilan yaratadi.
         */
        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "BirZum24 buyurtmalar",
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.setDescription(
                "Yangi buyurtmalar haqida bildirishnomalar"
        );

        channel.enableVibration(true);

        channel.setVibrationPattern(
                new long[]{
                        0,
                        300,
                        200,
                        500
                }
        );

        channel.setLockscreenVisibility(
                android.app.Notification.VISIBILITY_PUBLIC
        );

        /*
         * Default notification sound
         */
        Uri sound =
                android.provider.Settings.System
                        .DEFAULT_NOTIFICATION_URI;

        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(
                                AudioAttributes.USAGE_NOTIFICATION
                        )
                        .setContentType(
                                AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build();

        channel.setSound(
                sound,
                audioAttributes
        );

        manager.createNotificationChannel(
                channel
        );
    }
}
