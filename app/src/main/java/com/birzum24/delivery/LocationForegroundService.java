package com.birzum24.delivery;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class LocationForegroundService extends Service {

    private static final String TAG =
            "BirZumLocationService";


    private static final String CHANNEL_ID =
            "birzum_location";


    private static final int NOTIFICATION_ID =
            2401;


    private LocationManager locationManager;

    private LocationListener locationListener;


    private SharedPreferences prefs;


    @Override
    public void onCreate() {

        super.onCreate();


        prefs =
                getSharedPreferences(
                        "birzum_delivery",
                        MODE_PRIVATE
                );


        createNotificationChannel();


        startForeground(
                NOTIFICATION_ID,
                createNotification()
        );


        startLocationUpdates();
    }


    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(

                            CHANNEL_ID,

                            "BirZum24 joylashuv",

                            NotificationManager.IMPORTANCE_LOW
                    );


            channel.setDescription(
                    "Kuryer joylashuvini kuzatish"
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
    }


    private Notification createNotification() {

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );


        intent.setFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        );


        PendingIntent pendingIntent =
                PendingIntent.getActivity(

                        this,

                        2402,

                        intent,

                        PendingIntent.FLAG_UPDATE_CURRENT
                                |
                                PendingIntent.FLAG_IMMUTABLE
                );


        return new NotificationCompat.Builder(
                this,
                CHANNEL_ID
        )

                .setSmallIcon(
                        android.R.drawable.ic_menu_mylocation
                )

                .setContentTitle(
                        "BirZum24 Delivery"
                )

                .setContentText(
                        "Joylashuv faol"
                )

                .setOngoing(true)

                .setCategory(
                        NotificationCompat.CATEGORY_SERVICE
                )

                .setPriority(
                        NotificationCompat.PRIORITY_LOW
                )

                .setContentIntent(
                        pendingIntent
                )

                .build();
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


        locationManager =
                (LocationManager)
                        getSystemService(
                                Context.LOCATION_SERVICE
                        );


        if (locationManager == null) {

            stopSelf();

            return;
        }


        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            Location location
                    ) {

                        uploadLocation(
                                location
                        );
                    }


                    @Override
                    public void onProviderEnabled(
                            String provider
                    ) {
                    }


                    @Override
                    public void onProviderDisabled(
                            String provider
                    ) {
                    }
                };


        try {

            locationManager.requestLocationUpdates(

                    LocationManager.GPS_PROVIDER,

                    10000,

                    10,

                    locationListener,

                    Looper.getMainLooper()
            );


            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
                    ||
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {


                Location last =
                        locationManager.getLastKnownLocation(
                                LocationManager.GPS_PROVIDER
                        );


                if (last != null) {

                    uploadLocation(last);
                }
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "GPS start failed",
                    e
            );
        }
    }


    private void uploadLocation(
            Location location
    ) {

        final double lat =
                location.getLatitude();


        final double lng =
                location.getLongitude();


        String cookie =
                prefs.getString(
                        "cookie",
                        ""
                );


        String csrf =
                prefs.getString(
                        "csrf",
                        ""
                );


        if (cookie.isEmpty()) {

            Log.w(
                    TAG,
                    "No session cookie"
            );

            return;
        }


        if (csrf.isEmpty()) {

            Log.w(
                    TAG,
                    "No CSRF token"
            );

            return;
        }


        new Thread(() -> {

            boolean success =
                    LocationUploader.send(
                            lat,
                            lng,
                            cookie,
                            csrf
                    );


            Log.d(
                    TAG,
                    "Location: " +
                            lat +
                            ", " +
                            lng +
                            " success=" +
                            success
            );

        }).start();
    }


    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        return START_STICKY;
    }


    @Override
    public void onDestroy() {

        try {

            if (locationManager != null
                    &&
                    locationListener != null) {

                locationManager.removeUpdates(
                        locationListener
                );
            }

        } catch (Exception ignored) {
        }


        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
            }
