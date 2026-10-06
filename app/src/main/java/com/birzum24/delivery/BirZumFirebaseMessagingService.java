package com.birzum24.delivery;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class BirZumFirebaseMessagingService
        extends FirebaseMessagingService {

    /*
     * Yangi channel ID.
     *
     * Oldingi birzum_orders channelining Androiddagi
     * eski sozlamalarini chetlab o'tish uchun V2 ishlatyapmiz.
     */
    private static final String CHANNEL_ID =
            "birzum_orders_v2";

    private static final String PREFS =
            "birzum_delivery";

    @Override
    public void onNewToken(String token) {

        super.onNewToken(token);

        if (
                token == null ||
                token.trim().isEmpty()
        ) {
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        /*
         * Yangi Firebase tokenini saqlaymiz.
         */
        prefs.edit()
                .putString(
                        "fcm_token",
                        token
                )
                .remove(
                        "fcm_synced_token"
                )
                .putBoolean(
                        "fcm_synced",
                        false
                )
                .apply();
    }

    @Override
    public void onMessageReceived(
            RemoteMessage remoteMessage
    ) {

        super.onMessageReceived(
                remoteMessage
        );

        /*
         * Notification channelni oldindan yaratamiz.
         */
        createNotificationChannel();

        String title =
                "🚚 BirZum24";

        String body =
                "Sizga yangi buyurtma mavjud.";

        /*
         * Firebase notification payload.
         */
        if (
                remoteMessage.getNotification()
                        != null
        ) {

            String notificationTitle =
                    remoteMessage
                            .getNotification()
                            .getTitle();

            String notificationBody =
                    remoteMessage
                            .getNotification()
                            .getBody();

            if (
                    notificationTitle != null &&
                    !notificationTitle
                            .trim()
                            .isEmpty()
            ) {

                title =
                        notificationTitle;
            }

            if (
                    notificationBody != null &&
                    !notificationBody
                            .trim()
                            .isEmpty()
            ) {

                body =
                        notificationBody;
            }
        }

        /*
         * Firebase data payload.
         */
        String type =
                remoteMessage
                        .getData()
                        .get("type");

        String orderId =
                remoteMessage
                        .getData()
                        .get("order_id");

        /*
         * Agar data ichida title/body yuborilgan bo'lsa,
         * ularni ham qo'llab-quvvatlaymiz.
         */
        String dataTitle =
                remoteMessage
                        .getData()
                        .get("title");

        String dataBody =
                remoteMessage
                        .getData()
                        .get("body");

        if (
                dataTitle != null &&
                !dataTitle
                        .trim()
                        .isEmpty()
        ) {

            title =
                    dataTitle;
        }

        if (
                dataBody != null &&
                !dataBody
                        .trim()
                        .isEmpty()
        ) {

            body =
                    dataBody;
        }

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
         * Notification bosilganda MainActivity ochiladi.
         */
        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        if (
                type != null &&
                !type.trim().isEmpty()
        ) {

            intent.putExtra(
                    "notification_type",
                    type
            );
        }

        if (
                orderId != null &&
                !orderId.trim().isEmpty()
        ) {

            intent.putExtra(
                    "order_id",
                    orderId
            );
        }

        /*
         * Har bir buyurtma uchun alohida PendingIntent.
         */
        int requestCode;

        if (
                orderId != null &&
                !orderId.trim().isEmpty()
        ) {

            requestCode =
                    Math.abs(
                            orderId.hashCode()
                    );

        } else {

            requestCode =
                    (int)
                    (
                        System.currentTimeMillis()
                        & 0x7fffffff
                    );
        }

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
                );

        /*
         * Default Android notification sound.
         */
        Uri soundUri =
                RingtoneManager
                        .getDefaultUri(
                                RingtoneManager
                                        .TYPE_NOTIFICATION
                        );

        /*
         * Notification builder.
         */
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )

                /*
                 * Android notification icon.
                 */
                .setSmallIcon(
                        R.drawable.ic_launcher
                )

                .setContentTitle(
                        title
                )

                .setContentText(
                        body
                )

                .setStyle(
                        new NotificationCompat
                                .BigTextStyle()
                                .bigText(body)
                )

                /*
                 * Yuqori priority.
                 */
                .setPriority(
                        NotificationCompat
                                .PRIORITY_MAX
                )

                /*
                 * Message notification.
                 */
                .setCategory(
                        NotificationCompat
                                .CATEGORY_MESSAGE
                )

                /*
                 * Notification bosilganda
                 * avtomatik yopiladi.
                 */
                .setAutoCancel(
                        true
                )

                .setContentIntent(
                        pendingIntent
                )

                /*
                 * Ovoz.
                 */
                .setSound(
                        soundUri
                )

                /*
                 * Vibratsiya.
                 */
                .setVibrate(
                        new long[]{
                                0,
                                500,
                                250,
                                500
                        }
                )

                /*
                 * Lock screen'da ham ko'rinsin.
                 */
                .setVisibility(
                        NotificationCompat
                                .VISIBILITY_PUBLIC
                )

                /*
                 * Notificationni groupga biriktirmaymiz.
                 */
                .setGroup(
                        null
                )

                /*
                 * Timestamp.
                 */
                .setWhen(
                        System.currentTimeMillis()
                )

                .setShowWhen(
                        true
                );

        /*
         * Android 13+.
         */
        if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                    checkSelfPermission(
                            Manifest.permission
                                    .POST_NOTIFICATIONS
                    )
                    !=
                    PackageManager
                            .PERMISSION_GRANTED
            ) {

                /*
                 * Permission bo'lmasa notificationni
                 * Android ko'rsatmaydi.
                 */
                return;
            }
        }

        NotificationManagerCompat manager =
                NotificationManagerCompat
                        .from(this);

        /*
         * Notificationlar butunlay o'chirilgan
         * bo'lsa chiqarmaymiz.
         */
        if (
                !manager.areNotificationsEnabled()
        ) {

            return;
        }

        /*
         * Notification ID.
         */
        int notificationId;

        if (
                orderId != null &&
                !orderId.trim().isEmpty()
        ) {

            notificationId =
                    Math.abs(
                            orderId.hashCode()
                    );

            /*
             * Hash 0 bo'lib qolmasin.
             */
            if (notificationId == 0) {
                notificationId = 1;
            }

        } else {

            notificationId =
                    (int)
                    (
                        System.currentTimeMillis()
                        & 0x7fffffff
                    );
        }

        /*
         * Nihoyat notification.
         */
        manager.notify(
                notificationId,
                builder.build()
        );
    }

    private void createNotificationChannel() {

        /*
         * Android 8 dan oldin channel kerak emas.
         */
        if (
                Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O
        ) {

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
         * Default notification sound.
         */
        Uri soundUri =
                RingtoneManager
                        .getDefaultUri(
                                RingtoneManager
                                        .TYPE_NOTIFICATION
                        );

        /*
         * Audio attributes.
         */
        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(
                                AudioAttributes
                                        .USAGE_NOTIFICATION
                        )
                        .setContentType(
                                AudioAttributes
                                        .CONTENT_TYPE_SONIFICATION
                        )
                        .build();

        /*
         * HIGH importance.
         *
         * Bu pop-up/head-up notification uchun kerak.
         */
        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "BirZum24 buyurtmalar",
                        NotificationManager
                                .IMPORTANCE_HIGH
                );

        channel.setDescription(
                "BirZum24 yangi buyurtmalar"
        );

        /*
         * Sound.
         */
        channel.setSound(
                soundUri,
                audioAttributes
        );

        /*
         * Vibration.
         */
        channel.enableVibration(
                true
        );

        channel.setVibrationPattern(
                new long[]{
                        0,
                        500,
                        250,
                        500
                }
        );

        /*
         * Lock screen.
         */
        channel.setLockscreenVisibility(
                NotificationCompat
                        .VISIBILITY_PUBLIC
        );

        /*
         * Notification channel yaratish.
         */
        manager.createNotificationChannel(
                channel
        );
    }
}
