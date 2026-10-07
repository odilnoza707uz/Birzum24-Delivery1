package com.birzum24.delivery;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.webkit.CookieManager;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LocationForegroundService extends Service {

    private static final String CHANNEL_ID =
            "birzum_location";

    private static final int NOTIFICATION_ID = 2001;

    /*
     * Serverdagi kuryer sessiyasini faol ushlab turish.
     *
     * 45 sekundda bir marta state endpointga murojaat qiladi.
     */
    private static final long HEARTBEAT_INTERVAL =
            45 * 1000L;

    private static final String STATE_URL =
            "https://birzum.asakaedu.uz/c/api.php?a=state";

    private FusedLocationProviderClient fusedLocationClient;

    private LocationCallback locationCallback;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean destroyed = false;


    /*
     * =========================================================
     * HEARTBEAT
     * =========================================================
     */

    private final Runnable heartbeatRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (destroyed) {
                        return;
                    }

                    sendHeartbeat();

                    handler.postDelayed(
                            this,
                            HEARTBEAT_INTERVAL
                    );
                }
            };


    @Override
    public void onCreate() {
        super.onCreate();

        destroyed = false;

        createNotificationChannel();

        startForeground(
                NOTIFICATION_ID,
                createNotification()
        );

        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);

        startLocationUpdates();

        /*
         * Birinchi heartbeatni darhol yuboramiz.
         */
        sendHeartbeat();

        /*
         * Keyin har 45 sekundda.
         */
        handler.postDelayed(
                heartbeatRunnable,
                HEARTBEAT_INTERVAL
        );
    }


    /*
     * =========================================================
     * NOTIFICATION
     * =========================================================
     */

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
                        Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.M
                                ? android.app.PendingIntent.FLAG_IMMUTABLE
                                : 0
                );

        return new NotificationCompat.Builder(
                this,
                CHANNEL_ID
        )
                .setContentTitle(
                        "BirZum24"
                )
                .setContentText(
                        "Kuryer rejimi faol"
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
                .setContentIntent(
                        pendingIntent
                )
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
                "BirZum24 kuryer rejimi"
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


    /*
     * =========================================================
     * LOCATION
     * =========================================================
     */

    private void startLocationUpdates() {

        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {

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
                         * Hozircha lokatsiya shu yerda olinadi.
                         *
                         * Keyingi bosqichda serverga
                         * latitude/longitude yuboramiz.
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


    /*
     * =========================================================
     * SERVER HEARTBEAT
     * =========================================================
     */

    private void sendHeartbeat() {

        new Thread(
                () -> {

                    HttpURLConnection connection = null;

                    try {

                        URL url =
                                new URL(STATE_URL);

                        connection =
                                (HttpURLConnection)
                                        url.openConnection();

                        connection.setRequestMethod(
                                "GET"
                        );

                        connection.setConnectTimeout(
                                10000
                        );

                        connection.setReadTimeout(
                                10000
                        );

                        connection.setUseCaches(
                                false
                        );

                        /*
                         * WebView'dagi PHP session cookie.
                         */
                        String cookies =
                                CookieManager
                                        .getInstance()
                                        .getCookie(
                                                "https://birzum.asakaedu.uz"
                                        );

                        if (
                                cookies != null
                                        &&
                                !cookies.isEmpty()
                        ) {

                            connection.setRequestProperty(
                                    "Cookie",
                                    cookies
                            );
                        }

                        int responseCode =
                                connection.getResponseCode();

                        /*
                         * Javobni o'qib qo'yamiz.
                         * Connection yopilishidan oldin streamni
                         * bo'shatish foydali.
                         */
                        InputStream stream;

                        if (
                                responseCode >= 200
                                        &&
                                responseCode < 400
                        ) {

                            stream =
                                    connection.getInputStream();

                        } else {

                            stream =
                                    connection.getErrorStream();
                        }

                        if (stream != null) {
                            stream.close();
                        }

                        System.out.println(
                                "BIRZUM HEARTBEAT HTTP: "
                                        + responseCode
                        );

                    } catch (Exception e) {

                        System.out.println(
                                "BIRZUM HEARTBEAT ERROR: "
                                        + e.getMessage()
                        );

                    } finally {

                        if (connection != null) {
                            connection.disconnect();
                        }
                    }

                }
        ).start();
    }


    /*
     * =========================================================
     * START COMMAND
     * =========================================================
     */

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        /*
         * Android service'ni qayta yaratishga harakat qiladi.
         */
        return START_STICKY;
    }


    /*
     * =========================================================
     * DESTROY
     * =========================================================
     */

    @Override
    public void onDestroy() {

        destroyed = true;

        handler.removeCallbacksAndMessages(
                null
        );

        if (
                fusedLocationClient != null
                        &&
                locationCallback != null
        ) {

            fusedLocationClient
                    .removeLocationUpdates(
                            locationCallback
                    );
        }

        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(
            Intent intent
    ) {
        return null;
    }
}
