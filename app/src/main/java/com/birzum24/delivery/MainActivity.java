package com.birzum24.delivery;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL =
            "https://birzum.asakaedu.uz";

    private static final String START_URL =
            BASE_URL + "/c/";

    private static final String STATE_URL =
            BASE_URL + "/c/api.php?a=state";

    private static final String FCM_URL =
            BASE_URL + "/c/api.php?a=fcm_token";

    private static final int LOCATION_REQUEST = 1001;
    private static final int NOTIFICATION_REQUEST = 1002;
    private static final int CAMERA_REQUEST = 1003;
    private static final int BACKGROUND_LOCATION_REQUEST = 1011;

    private WebView webView;

    private View rootView;

    private SharedPreferences prefs;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean destroyed = false;

    private boolean sessionSyncRunning = false;

    private boolean locationPermissionRequested = false;

    private boolean cameraPermissionRequested = false;

    private boolean notificationPermissionRequested = false;


    // ============================================================
    // ACTIVITY CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        destroyed = false;

        /*
         * Android 15/16 edge-to-edge.
         *
         * System bar insetlarini rootLayout'ga beramiz.
         * WebView'ning o'ziga inset bermaymiz.
         */
        WindowCompat.setDecorFitsSystemWindows(
                getWindow(),
                false
        );

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP) {

            getWindow().setStatusBarColor(
                    Color.BLACK
            );

            getWindow().setNavigationBarColor(
                    Color.BLACK
            );
        }

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q) {

            getWindow().setStatusBarContrastEnforced(
                    false
            );

            getWindow().setNavigationBarContrastEnforced(
                    false
            );
        }

        prefs = getSharedPreferences(
                "birzum_delivery",
                MODE_PRIVATE
        );

        setContentView(
                R.layout.activity_main
        );

        /*
         * activity_main.xml:
         *
         * FrameLayout
         * id = rootLayout
         *
         * WebView
         * id = webView
         */
        rootView = findViewById(
                R.id.rootLayout
        );

        webView = findViewById(
                R.id.webView
        );

        if (rootView == null) {

            Toast.makeText(
                    this,
                    "rootLayout topilmadi.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (webView == null) {

            Toast.makeText(
                    this,
                    "WebView topilmadi.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        setupSystemBars();

        setupWebView();

        setupBackButton();

        requestNotificationPermission();

        requestLocationPermission();

        requestCameraPermission();

        getFcmToken();

        webView.loadUrl(
                START_URL
        );
    }


    // ============================================================
    // SYSTEM BAR INSETS
    // ============================================================

    private void setupSystemBars() {

        if (rootView == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(
                rootView,
                (view, insets) -> {

                    androidx.core.graphics.Insets systemInsets =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    /*
                     * ROOT:
                     *
                     * top    = status bar
                     * bottom = navigation bar
                     *
                     * WebView esa qolgan joyni egallaydi.
                     */
                    view.setPadding(
                            0,
                            systemInsets.top,
                            0,
                            systemInsets.bottom
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(
                rootView
        );
    }


    // ============================================================
    // WEBVIEW
    // ============================================================

    private void setupWebView() {

        if (webView == null) {
            return;
        }

        /*
         * System inset ROOT'da.
         *
         * WebView'ga alohida padding bermaymiz.
         */
        webView.setPadding(
                0,
                0,
                0,
                0
        );

        webView.setBackgroundColor(
                Color.WHITE
        );

        WebSettings settings =
                webView.getSettings();


        // --------------------------------------------------------
        // JAVASCRIPT
        // --------------------------------------------------------

        settings.setJavaScriptEnabled(
                true
        );


        // --------------------------------------------------------
        // DOM STORAGE
        // --------------------------------------------------------

        settings.setDomStorageEnabled(
                true
        );


        // --------------------------------------------------------
        // DATABASE
        // --------------------------------------------------------

        settings.setDatabaseEnabled(
                true
        );


        // --------------------------------------------------------
        // GEOLOCATION
        // --------------------------------------------------------

        settings.setGeolocationEnabled(
                true
        );


        // --------------------------------------------------------
        // VIEWPORT
        // --------------------------------------------------------

        settings.setUseWideViewPort(
                true
        );

        settings.setLoadWithOverviewMode(
                false
        );


        // --------------------------------------------------------
        // ZOOM
        // --------------------------------------------------------

        settings.setSupportZoom(
                false
        );

        settings.setBuiltInZoomControls(
                false
        );

        settings.setDisplayZoomControls(
                false
        );


        // --------------------------------------------------------
        // CACHE
        // --------------------------------------------------------

        settings.setCacheMode(
                WebSettings.LOAD_DEFAULT
        );


        // --------------------------------------------------------
        // FILE ACCESS
        // --------------------------------------------------------

        settings.setAllowFileAccess(
                true
        );

        settings.setAllowContentAccess(
                true
        );


        // --------------------------------------------------------
        // MIXED CONTENT
        // --------------------------------------------------------

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP) {

            settings.setMixedContentMode(
                    WebSettings.MIXED_CONTENT_NEVER_ALLOW
            );
        }


        // --------------------------------------------------------
        // COOKIES
        // --------------------------------------------------------

        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(
                true
        );

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP) {

            cookieManager.setAcceptThirdPartyCookies(
                    webView,
                    true
            );
        }


        // --------------------------------------------------------
        // USER AGENT
        // --------------------------------------------------------

        String userAgent =
                settings.getUserAgentString();

        if (userAgent != null &&
                !userAgent.contains(
                        "BirZum24Delivery"
                )) {

            settings.setUserAgentString(
                    userAgent +
                    " BirZum24Delivery/1.0"
            );
        }


        // ========================================================
        // WEBVIEW CLIENT
        // ========================================================

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request
                    ) {

                        if (request == null ||
                                request.getUrl() == null) {

                            return false;
                        }

                        String url =
                                request.getUrl()
                                        .toString();

                        /*
                         * BirZum24 sayti WebView ichida.
                         */
                        if (url.startsWith(
                                "https://birzum.asakaedu.uz"
                        )) {

                            return false;
                        }

                        /*
                         * HTTPS.
                         */
                        if (url.startsWith(
                                "https://"
                        )) {

                            return false;
                        }

                        /*
                         * HTTP.
                         */
                        if (url.startsWith(
                                "http://"
                        )) {

                            return false;
                        }

                        return false;
                    }


                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {

                        super.onPageFinished(
                                view,
                                url
                        );

                        syncSessionState();
                    }
                }
        );


        // ========================================================
        // WEB CHROME CLIENT
        // ========================================================

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onPermissionRequest(
                            PermissionRequest request
                    ) {

                        runOnUiThread(
                                () -> {

                                    if (request == null) {
                                        return;
                                    }

                                    String[] resources =
                                            request.getResources();

                                    if (resources == null ||
                                            resources.length == 0) {

                                        request.deny();

                                        return;
                                    }

                                    boolean camera =
                                            false;

                                    boolean microphone =
                                            false;

                                    for (
                                            String resource :
                                            resources
                                    ) {

                                        if (
                                                PermissionRequest
                                                        .RESOURCE_VIDEO_CAPTURE
                                                        .equals(
                                                                resource
                                                        )
                                        ) {

                                            camera = true;
                                        }

                                        if (
                                                PermissionRequest
                                                        .RESOURCE_AUDIO_CAPTURE
                                                        .equals(
                                                                resource
                                                        )
                                        ) {

                                            microphone = true;
                                        }
                                    }

                                    boolean cameraGranted =
                                            ContextCompat.checkSelfPermission(
                                                    MainActivity.this,
                                                    Manifest.permission.CAMERA
                                            ) ==
                                            PackageManager.PERMISSION_GRANTED;

                                    /*
                                     * Camera ruxsati bor.
                                     */
                                    if (
                                            camera &&
                                            cameraGranted
                                    ) {

                                        request.grant(
                                                resources
                                        );

                                        return;
                                    }

                                    /*
                                     * Microphone uchun
                                     * hozircha ruxsat yo'q.
                                     */
                                    if (microphone) {

                                        request.deny();

                                        return;
                                    }

                                    request.deny();
                                }
                        );
                    }


                    @Override
                    public void onGeolocationPermissionsShowPrompt(
                            String origin,
                            GeolocationPermissions.Callback callback
                    ) {

                        boolean fine =
                                ContextCompat.checkSelfPermission(
                                        MainActivity.this,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                ) ==
                                PackageManager.PERMISSION_GRANTED;

                        boolean coarse =
                                ContextCompat.checkSelfPermission(
                                        MainActivity.this,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                ) ==
                                PackageManager.PERMISSION_GRANTED;

                        boolean granted =
                                fine || coarse;

                        if (granted) {

                            callback.invoke(
                                    origin,
                                    true,
                                    false
                            );

                        } else {

                            callback.invoke(
                                    origin,
                                    false,
                                    false
                            );
                        }
                    }
                }
        );


        webView.setFocusable(
                true
        );

        webView.setFocusableInTouchMode(
                true
        );

        webView.setOverScrollMode(
                View.OVER_SCROLL_NEVER
        );
    }


    // ============================================================
    // BACK BUTTON
    // ============================================================

    private void setupBackButton() {

        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {

                            @Override
                            public void handleOnBackPressed() {

                                if (
                                        webView != null &&
                                        webView.canGoBack()
                                ) {

                                    webView.goBack();

                                } else {

                                    finish();
                                }
                            }
                        }
                );
    }


    // ============================================================
    // NOTIFICATION PERMISSION
    // ============================================================

    private void requestNotificationPermission() {

        if (
                Build.VERSION.SDK_INT <
                Build.VERSION_CODES.TIRAMISU
        ) {

            return;
        }

        if (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                ) ==
                PackageManager.PERMISSION_GRANTED
        ) {

            return;
        }

        if (notificationPermissionRequested) {
            return;
        }

        notificationPermissionRequested = true;

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.POST_NOTIFICATIONS
                },
                NOTIFICATION_REQUEST
        );
    }


    // ============================================================
    // LOCATION PERMISSION
    // ============================================================

    private void requestLocationPermission() {

        if (locationPermissionRequested) {
            return;
        }

        boolean fine =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED;

        boolean coarse =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {

            startLocationServiceIfPossible();

            return;
        }

        locationPermissionRequested = true;

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_REQUEST
        );
    }


    // ============================================================
    // CAMERA PERMISSION
    // ============================================================

    private void requestCameraPermission() {

        if (cameraPermissionRequested) {
            return;
        }

        if (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                ) ==
                PackageManager.PERMISSION_GRANTED
        ) {

            return;
        }

        cameraPermissionRequested = true;

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.CAMERA
                },
                CAMERA_REQUEST
        );
    }


    // ============================================================
    // BACKGROUND LOCATION
    // ============================================================

    private void requestBackgroundLocationPermission() {

        if (
                Build.VERSION.SDK_INT <
                Build.VERSION_CODES.Q
        ) {

            startLocationServiceIfPossible();

            return;
        }

        if (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED
        ) {

            startLocationServiceIfPossible();

            return;
        }

        new Handler(
                Looper.getMainLooper()
        ).postDelayed(
                () -> {

                    if (
                            isFinishing() ||
                            isDestroyed()
                    ) {

                        return;
                    }

                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{
                                    Manifest.permission
                                            .ACCESS_BACKGROUND_LOCATION
                            },
                            BACKGROUND_LOCATION_REQUEST
                    );

                },
                500
        );
    }


    // ============================================================
    // START LOCATION SERVICE
    // ============================================================

    private void startLocationServiceIfPossible() {

        boolean fine =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED;

        boolean coarse =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            return;
        }

        try {

            Intent serviceIntent =
                    new Intent(
                            this,
                            LocationForegroundService.class
                    );

            if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O
            ) {

                ContextCompat.startForegroundService(
                        this,
                        serviceIntent
                );

            } else {

                startService(
                        serviceIntent
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Lokatsiya xizmati ishga tushmadi.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // ============================================================
    // FCM TOKEN
    // ============================================================

    private void getFcmToken() {

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnCompleteListener(
                        task -> {

                            if (!task.isSuccessful()) {

                                return;
                            }

                            String token =
                                    task.getResult();

                            if (
                                    token == null ||
                                    token.trim().isEmpty()
                            ) {

                                return;
                            }

                            saveAndSyncFcmToken(
                                    token
                            );
                        }
                );
    }


    // ============================================================
    // SAVE FCM TOKEN
    // ============================================================

    private void saveAndSyncFcmToken(
            String token
    ) {

        if (
                token == null ||
                token.trim().isEmpty()
        ) {

            return;
        }

        token = token.trim();

        String syncedToken =
                prefs.getString(
                        "fcm_synced_token",
                        ""
                );

        prefs.edit()
                .putString(
                        "fcm_token",
                        token
                )
                .apply();

        /*
         * Oldin serverga yuborilgan bo'lsa
         * qayta yubormaymiz.
         */
        if (
                token.equals(
                        syncedToken
                )
        ) {

            return;
        }

        if (sessionSyncRunning) {
            return;
        }

        syncFcmTokenToServer(
                token
        );
    }


    // ============================================================
    // SEND FCM TOKEN TO PHP
    // ============================================================

    private void syncFcmTokenToServer(
            String token
    ) {

        if (
                token == null ||
                token.trim().isEmpty()
        ) {

            return;
        }

        if (sessionSyncRunning) {
            return;
        }

        sessionSyncRunning = true;

        final String finalToken =
                token.trim();

        new Thread(
                () -> {

                    HttpURLConnection connection =
                            null;

                    try {

                        URL url =
                                new URL(
                                        FCM_URL
                                );

                        connection =
                                (HttpURLConnection)
                                        url.openConnection();

                        connection.setRequestMethod(
                                "POST"
                        );

                        connection.setConnectTimeout(
                                15000
                        );

                        connection.setReadTimeout(
                                15000
                        );

                        connection.setDoOutput(
                                true
                        );

                        connection.setRequestProperty(
                                "Content-Type",
                                "application/json; charset=UTF-8"
                        );

                        connection.setRequestProperty(
                                "Accept",
                                "application/json"
                        );


                        // ------------------------------------------------
                        // COOKIE
                        // ------------------------------------------------

                        String cookies =
                                CookieManager
                                        .getInstance()
                                        .getCookie(
                                                BASE_URL
                                        );

                        if (
                                cookies != null &&
                                !cookies.isEmpty()
                        ) {

                            connection.setRequestProperty(
                                    "Cookie",
                                    cookies
                            );
                        }


                        // ------------------------------------------------
                        // JSON
                        // ------------------------------------------------

                        JSONObject json =
                                new JSONObject();

                        json.put(
                                "token",
                                finalToken
                        );

                        byte[] body =
                                json.toString()
                                        .getBytes(
                                                StandardCharsets.UTF_8
                                        );


                        OutputStream output =
                                connection.getOutputStream();

                        output.write(
                                body
                        );

                        output.flush();

                        output.close();


                        // ------------------------------------------------
                        // RESPONSE
                        // ------------------------------------------------

                        int responseCode =
                                connection.getResponseCode();

                        InputStream stream;

                        if (
                                responseCode >= 200 &&
                                responseCode < 400
                        ) {

                            stream =
                                    connection.getInputStream();

                        } else {

                            stream =
                                    connection.getErrorStream();
                        }

                        String response =
                                readStream(
                                        stream
                                );

                        final String finalResponse =
                                response == null
                                        ? ""
                                        : response;


                        runOnUiThread(
                                () -> {

                                    sessionSyncRunning =
                                            false;

                                    try {

                                        JSONObject result =
                                                new JSONObject(
                                                        finalResponse
                                                );

                                        boolean ok =
                                                result.optBoolean(
                                                        "ok",
                                                        false
                                                );

                                        /*
                                         * PHP true yoki 1
                                         * bo'lishi mumkin.
                                         */
                                        boolean saved =
                                                result.optBoolean(
                                                        "saved",
                                                        false
                                                )
                                                ||
                                                result.optInt(
                                                        "saved",
                                                        0
                                                ) == 1;

                                        if (
                                                ok &&
                                                saved
                                        ) {

                                            prefs.edit()
                                                    .putString(
                                                            "fcm_synced_token",
                                                            finalToken
                                                    )
                                                    .apply();
                                        }

                                    } catch (
                                            Exception e
                                    ) {

                                        e.printStackTrace();
                                    }
                                }
                        );

                    } catch (
                            Exception e
                    ) {

                        e.printStackTrace();

                        runOnUiThread(
                                () -> {

                                    sessionSyncRunning =
                                            false;
                                }
                        );

                    } finally {

                        if (connection != null) {

                            connection.disconnect();
                        }
                    }

                }
        ).start();
    }


    // ============================================================
    // SESSION STATE
    // ============================================================

    private void syncSessionState() {

        if (destroyed) {
            return;
        }

        new Thread(
                () -> {

                    HttpURLConnection connection =
                            null;

                    try {

                        URL url =
                                new URL(
                                        STATE_URL
                                );

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


                        // --------------------------------------------
                        // COOKIE
                        // --------------------------------------------

                        String cookies =
                                CookieManager
                                        .getInstance()
                                        .getCookie(
                                                BASE_URL
                                        );

                        if (
                                cookies != null &&
                                !cookies.isEmpty()
                        ) {

                            connection.setRequestProperty(
                                    "Cookie",
                                    cookies
                            );
                        }


                        int responseCode =
                                connection.getResponseCode();

                        if (
                                responseCode >= 200 &&
                                responseCode < 400
                        ) {

                            InputStream stream =
                                    connection.getInputStream();

                            String response =
                                    readStream(
                                            stream
                                    );

                            if (
                                    response != null &&
                                    !response.isEmpty()
                            ) {

                                try {

                                    new JSONObject(
                                            response
                                    );

                                } catch (
                                        Exception ignored
                                ) {
                                }
                            }
                        }

                    } catch (
                            Exception e
                    ) {

                        e.printStackTrace();

                    } finally {

                        if (connection != null) {

                            connection.disconnect();
                        }
                    }

                }
        ).start();
    }


    // ============================================================
    // READ INPUT STREAM
    // ============================================================

    private String readStream(
            InputStream inputStream
    ) {

        if (inputStream == null) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    StandardCharsets.UTF_8
                            )
                    );

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                builder.append(
                        line
                );
            }

            reader.close();

        } catch (
                Exception e
        ) {

            e.printStackTrace();
        }

        return builder.toString();
    }


    // ============================================================
    // PERMISSION RESULT
    // ============================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );


        // --------------------------------------------------------
        // LOCATION
        // --------------------------------------------------------

        if (
                requestCode ==
                LOCATION_REQUEST
        ) {

            boolean granted =
                    false;

            if (
                    grantResults != null
            ) {

                for (
                        int result :
                        grantResults
                ) {

                    if (
                            result ==
                            PackageManager.PERMISSION_GRANTED
                    ) {

                        granted = true;

                        break;
                    }
                }
            }

            if (granted) {

                startLocationServiceIfPossible();

                if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.Q
                ) {

                    requestBackgroundLocationPermission();
                }

            } else {

                Toast.makeText(
                        this,
                        "Lokatsiya ruxsati berilmadi.",
                        Toast.LENGTH_LONG
                ).show();
            }

            return;
        }


        // --------------------------------------------------------
        // BACKGROUND LOCATION
        // --------------------------------------------------------

        if (
                requestCode ==
                BACKGROUND_LOCATION_REQUEST
        ) {

            startLocationServiceIfPossible();

            return;
        }


        // --------------------------------------------------------
        // CAMERA
        // --------------------------------------------------------

        if (
                requestCode ==
                CAMERA_REQUEST
        ) {

            boolean granted =
                    grantResults != null &&
                    grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;

            if (!granted) {

                Toast.makeText(
                        this,
                        "Kamera ruxsati berilmadi.",
                        Toast.LENGTH_SHORT
                ).show();
            }

            return;
        }


        // --------------------------------------------------------
        // NOTIFICATION
        // --------------------------------------------------------

        if (
                requestCode ==
                NOTIFICATION_REQUEST
        ) {

            boolean granted =
                    grantResults != null &&
                    grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;

            if (!granted) {

                Toast.makeText(
                        this,
                        "Bildirishnoma ruxsati berilmadi.",
                        Toast.LENGTH_LONG
                ).show();
            }

            return;
        }
    }


    // ============================================================
    // RESUME
    // ============================================================

    @Override
    protected void onResume() {

        super.onResume();

        destroyed = false;

        getFcmToken();

        startLocationServiceIfPossible();
    }


    // ============================================================
    // PAUSE
    // ============================================================

    @Override
    protected void onPause() {

        super.onPause();

        /*
         * Location service to'xtatilmaydi.
         */
    }


    // ============================================================
    // DESTROY
    // ============================================================

    @Override
    protected void onDestroy() {

        destroyed = true;

        if (webView != null) {

            webView.stopLoading();

            webView.setWebViewClient(
                    null
            );

            webView.setWebChromeClient(
                    null
            );

            webView.destroy();

            webView = null;
        }

        handler.removeCallbacksAndMessages(
                null
        );

        super.onDestroy();
    }
}
