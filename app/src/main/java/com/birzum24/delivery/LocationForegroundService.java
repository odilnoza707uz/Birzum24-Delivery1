package com.birzum24.delivery;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationForegroundService extends Service {

    private static final String CHANNEL_ID =
            "birzum_location";

    private static final int NOTIFICATION_ID = 2001;

    private FusedLocationProviderClient fusedLocationClient;

    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        startForeground(
                NOTIFICATION_ID,
                createNotification()
        );

        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);

        startLocationUpdates();
    }

    private Notification createNotification() {

        Intent notificationIntent =
                new Intent(
                        this,
                        MainActivity.class
                );

        android.app.PendingIntent pendingIntent =
                android.app.PendingIntent.getActivity(
                        this,
                        0,
                        notificationIntent,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                ? android.app.PendingIntent.FLAG_IMMUTABLE
                                : 0
                );

        return new NotificationCompat.Builder(
                this,
                CHANNEL_ID
        )
                .setContentTitle("BirZum24")
                .setContentText(
                        "Kuryer joylashuvi faol ishlamoqda"
                )
                .setSmallIcon(
                        R.drawable.ic_launcher
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(
                        NotificationCompat.CATEGORY_SERVICE
                )
                .setPriority(
                        NotificationCompat.PRIORITY_LOW
                )
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Kuryer joylashuvi",
                        NotificationManager.IMPORTANCE_LOW
                );

        channel.setDescription(
                "BirZum24 kuryer joylashuvini fonda kuzatish"
        );

        channel.setShowBadge(false);

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        if (manager != null) {

            manager.createNotificationChannel(
                    channel
            );
        }
    }

    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            stopSelf();
            return;
        }

        LocationRequest request =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        5000
                )
                        .setMinUpdateIntervalMillis(3000)
                        .setMaxUpdateDelayMillis(10000)
                        .setWaitForAccurateLocation(false)
                        .build();

        locationCallback =
                new LocationCallback() {

                    @Override
                    public void onLocationResult(
                            LocationResult result
                    ) {

                        if (result == null) {
                            return;
                        }

                        android.location.Location location =
                                result.getLastLocation();

                        if (location == null) {
                            return;
                        }

                        double latitude =
                                location.getLatitude();

                        double longitude =
                                location.getLongitude();

                        /*
                         * Hozircha shu yerda lokatsiya olinadi.
                         *
                         * Keyingi bosqichda:
                         * latitude + longitude
                         * -> PHP API
                         * -> bz_couriers
                         * orqali serverga yuboramiz.
                         */
                        System.out.println(
                                "BIRZUM LOCATION: "
                                        + latitude
                                        + ", "
                                        + longitude
                        );
                    }
                };

        fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        /*
         * Android service'ni o'ldirsa,
         * imkon bo'lsa qayta yaratadi.
         */
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        if (fusedLocationClient != null
                && locationCallback != null) {

            fusedLocationClient
                    .removeLocationUpdates(
                            locationCallback
                    );
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
