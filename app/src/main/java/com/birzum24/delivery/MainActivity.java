package com.birzum24.delivery;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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

    private static final String URL =
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

    private SharedPreferences prefs;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean sessionSyncRunning = false;

    private boolean destroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        destroyed = false;

        /*
         * Edge-to-edge ni o'chiramiz.
         * WebView status bar ostiga kirib ketmasligi uchun
         * pastdagi WindowInsets ham qo'llanadi.
         */
        WindowCompat.setDecorFitsSystemWindows(
                getWindow(),
                true
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        prefs = getSharedPreferences(
                "birzum_delivery",
                MODE_PRIVATE
        );

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);

        setupSystemBarInsets();

        setupWebView();

        setupBackButton();

        requestNotificationPermission();

        requestLocationPermission();

        requestCameraPermission();

        getFcmToken();

        webView.loadUrl(URL);
    }

    // =========================================================
    // SYSTEM BAR / STATUS BAR
    // =========================================================

    private void setupSystemBarInsets() {

        if (webView == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(
                webView,
                (view, insets) -> {

                    WindowInsetsCompat.Type.InsetsType type =
                            WindowInsetsCompat.Type.systemBars();

                    androidx.core.graphics.Insets systemInsets =
                            insets.getInsets(type);

                    view.setPadding(
                            0,
                            systemInsets.top,
                            0,
                            systemInsets.bottom
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(webView);
    }

    // =========================================================
    // WEBVIEW
    // =========================================================

    private void setupWebView() {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);

        settings.setDomStorageEnabled(true);

        settings.setDatabaseEnabled(true);

        settings.setGeolocationEnabled(true);

        settings.setAllowFileAccess(true);

        settings.setAllowContentAccess(true);

        settings.setJavaScriptCanOpenWindowsAutomatically(
                true
        );

        settings.setSupportMultipleWindows(false);

        settings.setMediaPlaybackRequiresUserGesture(
                false
        );

        settings.setBuiltInZoomControls(false);

        settings.setDisplayZoomControls(false);

        settings.setLoadWithOverviewMode(false);

        settings.setUseWideViewPort(false);

        /*
         * Cookie.
         */
        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP) {

            cookieManager.setAcceptThirdPartyCookies(
                    webView,
                    true
            );
        }

        /*
         * WebView client.
         */
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

                        String host =
                                request.getUrl().getHost();

                        if (host != null &&
                                host.equals(
                                        "birzum.asakaedu.uz"
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

                        /*
                         * Cookie yozilishi uchun sync.
                         */
                        CookieManager
                                .getInstance()
                                .flush();

                        /*
                         * Sayt yuklangandan keyin
                         * session/token tekshiriladi.
                         */
                        handler.postDelayed(
                                MainActivity.this
                                        ::syncSessionAndTracking,
                                1200
                        );

                        handler.postDelayed(
                                MainActivity.this
                                        ::syncSessionAndTracking,
                                4500
                        );
                    }
                }
        );

        /*
         * Chrome client.
         */
        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onGeolocationPermissionsShowPrompt(
                            String origin,
                            GeolocationPermissions.Callback callback
                    ) {

                        if (hasLocationPermission()) {

                            callback.invoke(
                                    origin,
                                    true,
                                    false
                            );

                        } else {

                            requestLocationPermission();

                            callback.invoke(
                                    origin,
                                    false,
                                    false
                            );
                        }
                    }

                    @Override
                    public void onPermissionRequest(
                            PermissionRequest request
                    ) {

                        runOnUiThread(() -> {

                            String[] resources =
                                    request.getResources();

                            boolean camera =
                                    false;

                            for (String resource :
                                    resources) {

                                if (PermissionRequest
                                        .RESOURCE_VIDEO_CAPTURE
                                        .equals(resource)) {

                                    camera = true;
                                    break;
                                }
                            }

                            if (!camera) {

                                request.grant(
                                        resources
                                );

                                return;
                            }

                            if (
                                    ContextCompat
                                            .checkSelfPermission(
                                                    MainActivity.this,
                                                    Manifest.permission.CAMERA
                                            )
                                            ==
                                            PackageManager.PERMISSION_GRANTED
                            ) {

                                request.grant(
                                        resources
                                );

                            } else {

                                requestCameraPermission();
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // FCM TOKEN
    // =========================================================

    private void getFcmToken() {

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnCompleteListener(
                        task -> {

                            if (!task.isSuccessful()) {

                                String error =
                                        task.getException() != null
                                                ? task.getException()
                                                        .getMessage()
                                                : "Noma'lum Firebase xatosi";

                                Toast.makeText(
                                        MainActivity.this,
                                        "FCM TOKEN XATO:\n"
                                                + error,
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            String token =
                                    task.getResult();

                            if (
                                    token == null ||
                                    token.trim().isEmpty()
                            ) {

                                Toast.makeText(
                                        MainActivity.this,
                                        "FCM TOKEN BO‘SH",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            prefs.edit()
                                    .putString(
                                            "fcm_token",
                                            token
                                    )
                                    .apply();

                            /*
                             * Token olingan.
                             * Sayt sessioni tayyor bo'lganda
                             * serverga yuboriladi.
                             */
                            syncSessionAndTracking();
                        }
                );
    }

    // =========================================================
    // SESSION + FCM + LOCATION
    // =========================================================

    private void syncSessionAndTracking() {

        if (destroyed) {
            return;
        }

        if (sessionSyncRunning) {
            return;
        }

        if (webView == null) {
            return;
        }

        sessionSyncRunning = true;

        /*
         * WebView cookie.
         */
        CookieManager cookieManager =
                CookieManager.getInstance();

        String cookie =
                cookieManager.getCookie(
                        BASE_URL
                );

        if (
                cookie == null ||
                cookie.trim().isEmpty()
        ) {

            cookie =
                    cookieManager.getCookie(
                            URL
                    );
        }

        if (
                cookie == null ||
                cookie.trim().isEmpty()
        ) {

            sessionSyncRunning = false;

            /*
             * Sayt hali cookie bermagan bo'lishi mumkin.
             * Keyin yana urinib ko'ramiz.
             */
            handler.postDelayed(
                    this::syncSessionAndTracking,
                    2500
            );

            return;
        }

        final String finalCookie =
                cookie;

        /*
         * Native HTTP.
         *
         * WebView fetch ishlatilmaydi.
         */
        new Thread(() -> {

            String responseText = "";

            int httpCode = 0;

            String error = "";

            try {

                HttpURLConnection connection =
                        (HttpURLConnection)
                                new URL(
                                        STATE_URL
                                ).openConnection();

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
                        "Cookie",
                        finalCookie
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "BirZum24-Delivery-Android"
                );

                byte[] body =
                        "{}".getBytes(
                                StandardCharsets.UTF_8
                        );

                try (OutputStream output =
                             connection.getOutputStream()) {

                    output.write(body);
                    output.flush();
                }

                httpCode =
                        connection.getResponseCode();

                InputStream stream;

                if (httpCode >= 200 &&
                        httpCode < 400) {

                    stream =
                            connection.getInputStream();

                } else {

                    stream =
                            connection.getErrorStream();
                }

                if (stream != null) {

                    responseText =
                            readStream(stream);
                }

                connection.disconnect();

            } catch (Exception e) {

                error =
                        e.getClass().getSimpleName()
                                + ": "
                                + e.getMessage();
            }

            final int finalHttpCode =
                    httpCode;

            final String finalResponse =
                    responseText;

            final String finalError =
                    error;

            runOnUiThread(() -> {

                sessionSyncRunning = false;

                if (destroyed) {
                    return;
                }

                if (!finalError.isEmpty()) {

                    /*
                     * Debug uchun faqat Toast.
                     */
                    Toast.makeText(
                            MainActivity.this,
                            "STATE XATO:\n"
                                    + finalError,
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                if (finalResponse.isEmpty()) {

                    Toast.makeText(
                            MainActivity.this,
                            "STATE javobi bo‘sh. HTTP "
                                    + finalHttpCode,
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                try {

                    JSONObject data =
                            new JSONObject(
                                    finalResponse
                            );

                    boolean ok =
                            data.optBoolean(
                                    "ok",
                                    false
                            );

                    if (!ok) {

                        Toast.makeText(
                                MainActivity.this,
                                "SESSION OK = FALSE\n"
                                        + finalResponse,
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    String csrf =
                            data.optString(
                                    "csrf",
                                    ""
                            );

                    String stage =
                            data.optString(
                                    "stage",
                                    "login"
                            );

                    /*
                     * Session ma'lumotlarini saqlaymiz.
                     */
                    prefs.edit()
                            .putString(
                                    "cookie",
                                    finalCookie
                            )
                            .putString(
                                    "csrf",
                                    csrf
                            )
                            .putString(
                                    "stage",
                                    stage
                            )
                            .apply();

                    /*
                     * Faqat kuryer tizimiga kirgan bo'lsa.
                     */
                    if (
                            "ready".equals(
                                    stage
                            )
                    ) {

                        String token =
                                prefs.getString(
                                        "fcm_token",
                                        ""
                                );

                        if (
                                token != null &&
                                !token.trim().isEmpty() &&
                                csrf != null &&
                                !csrf.trim().isEmpty()
                        ) {

                            syncFcmTokenToServer(
                                    finalCookie,
                                    csrf,
                                    token
                            );
                        }

                        if (
                                hasLocationPermission()
                        ) {

                            startLocationService();
                        }
                    }

                } catch (Exception e) {

                    Toast.makeText(
                            MainActivity.this,
                            "STATE JSON XATO:\n"
                                    + e.getMessage()
                                    + "\nHTTP "
                                    + finalHttpCode,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });

        }).start();
    }

    // =========================================================
    // FCM TOKEN -> PHP
    // =========================================================

    private void syncFcmTokenToServer(
            String cookie,
            String csrf,
            String token
    ) {

        if (
                cookie == null ||
                cookie.trim().isEmpty()
        ) {
            return;
        }

        if (
                csrf == null ||
                csrf.trim().isEmpty()
        ) {
            return;
        }

        if (
                token == null ||
                token.trim().isEmpty()
        ) {
            return;
        }

        new Thread(() -> {

            String responseText = "";

            int httpCode = 0;

            String error = "";

            try {

                JSONObject body =
                        new JSONObject();

                body.put(
                        "token",
                        token
                );

                byte[] bodyBytes =
                        body.toString()
                                .getBytes(
                                        StandardCharsets.UTF_8
                                );

                HttpURLConnection connection =
                        (HttpURLConnection)
                                new URL(
                                        FCM_URL
                                ).openConnection();

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
                        "Cookie",
                        cookie
                );

                connection.setRequestProperty(
                        "X-CSRF-Token",
                        csrf
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "BirZum24-Delivery-Android"
                );

                try (OutputStream output =
                             connection.getOutputStream()) {

                    output.write(
                            bodyBytes
                    );

                    output.flush();
                }

                httpCode =
                        connection.getResponseCode();

                InputStream stream;

                if (httpCode >= 200 &&
                        httpCode < 400) {

                    stream =
                            connection.getInputStream();

                } else {

                    stream =
                            connection.getErrorStream();
                }

                if (stream != null) {

                    responseText =
                            readStream(stream);
                }

                connection.disconnect();

            } catch (Exception e) {

                error =
                        e.getClass().getSimpleName()
                                + ": "
                                + e.getMessage();
            }

            final int finalHttpCode =
                    httpCode;

            final String finalResponse =
                    responseText;

            final String finalError =
                    error;

            runOnUiThread(() -> {

                if (destroyed) {
                    return;
                }

                if (!finalError.isEmpty()) {

                    Toast.makeText(
                            MainActivity.this,
                            "FCM SERVER XATO:\n"
                                    + finalError,
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                if (finalResponse.isEmpty()) {

                    Toast.makeText(
                            MainActivity.this,
                            "FCM server javobi bo‘sh.\nHTTP "
                                    + finalHttpCode,
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                try {

                    JSONObject json =
                            new JSONObject(
                                    finalResponse
                            );

                    boolean ok =
                            json.optBoolean(
                                    "ok",
                                    false
                            );

                    boolean saved =
                            json.optBoolean(
                                    "saved",
                                    false
                            );

                    if (
                            ok &&
                            saved
                    ) {

                        prefs.edit()
                                .putBoolean(
                                        "fcm_synced",
                                        true
                                )
                                .apply();

                        Toast.makeText(
                                MainActivity.this,
                                "✅ FCM BAZAGA SAQLANDI",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        Toast.makeText(
                                MainActivity.this,
                                "❌ FCM BAZAGA SAQLANMADI\n"
                                        + finalResponse,
                                Toast.LENGTH_LONG
                        ).show();
                    }

                } catch (Exception e) {

                    Toast.makeText(
                            MainActivity.this,
                            "FCM JSON XATO:\n"
                                    + e.getMessage()
                                    + "\n"
                                    + finalResponse,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });

        }).start();
    }

    // =========================================================
    // READ HTTP STREAM
    // =========================================================

    private String readStream(
            InputStream stream
    ) throws Exception {

        StringBuilder result =
                new StringBuilder();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        stream,
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                result.append(line);
            }
        }

        return result.toString();
    }

    // =========================================================
    // LOCATION PERMISSION
    // =========================================================

    private boolean hasLocationPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

                ||

                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {

        if (hasLocationPermission()) {

            requestBackgroundLocation();

            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_REQUEST
        );
    }

    // =========================================================
    // BACKGROUND LOCATION
    // =========================================================

    private void requestBackgroundLocation() {

        if (
                Build.VERSION.SDK_INT <
                        Build.VERSION_CODES.Q
        ) {
            return;
        }

        if (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                ==
                PackageManager.PERMISSION_GRANTED
        ) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Doimiy joylashuv"
                )
                .setMessage(
                        "BirZum24 kuryer ilovasi buyurtma vaqtida joylashuvingizni fonda ham yuborishi kerak. Keyingi oynada \"Har doim ruxsat berish\"ni tanlang."
                )
                .setPositiveButton(
                        "Ruxsat berish",
                        (dialog, which) -> {

                            if (
                                    Build.VERSION.SDK_INT >=
                                            Build.VERSION_CODES.Q
                            ) {

                                ActivityCompat.requestPermissions(
                                        this,
                                        new String[]{
                                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                        },
                                        BACKGROUND_LOCATION_REQUEST
                                );
                            }
                        }
                )
                .setNegativeButton(
                        "Keyin",
                        null
                )
                .show();
    }

    // =========================================================
    // NOTIFICATION
    // =========================================================

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
                )
                ==
                PackageManager.PERMISSION_GRANTED
        ) {
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.POST_NOTIFICATIONS
                },
                NOTIFICATION_REQUEST
        );
    }

    // =========================================================
    // CAMERA
    // =========================================================

    private void requestCameraPermission() {

        if (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                )
                ==
                PackageManager.PERMISSION_GRANTED
        ) {
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.CAMERA
                },
                CAMERA_REQUEST
        );
    }

    // =========================================================
    // LOCATION FOREGROUND SERVICE
    // =========================================================

    private void startLocationService() {

        try {

            Intent intent =
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
                        intent
                );

            } else {

                startService(
                        intent
                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "LOCATION SERVICE XATO:\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // PERMISSIONS RESULT
    // =========================================================

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

        if (
                requestCode ==
                        LOCATION_REQUEST
        ) {

            if (hasLocationPermission()) {

                requestBackgroundLocation();

                syncSessionAndTracking();

            } else {

                Toast.makeText(
                        this,
                        "Joylashuv ruxsati berilmadi",
                        Toast.LENGTH_LONG
                ).show();
            }

            return;
        }

        if (
                requestCode ==
                        BACKGROUND_LOCATION_REQUEST
        ) {

            if (hasLocationPermission()) {

                startLocationService();
            }

            syncSessionAndTracking();

            return;
        }

        if (
                requestCode ==
                        NOTIFICATION_REQUEST
        ) {

            if (
                    Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.TIRAMISU
            ) {

                if (
                        ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                        )
                        ==
                        PackageManager.PERMISSION_GRANTED
                ) {

                    Toast.makeText(
                            this,
                            "Bildirishnoma ruxsati berildi",
                            Toast.LENGTH_SHORT
                    ).show();

                } else {

                    Toast.makeText(
                            this,
                            "Bildirishnoma ruxsati berilmadi",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            return;
        }

        if (
                requestCode ==
                        CAMERA_REQUEST
        ) {

            if (
                    grantResults.length > 0
                    &&
                    grantResults[0]
                            ==
                            PackageManager.PERMISSION_GRANTED
            ) {

                Toast.makeText(
                        this,
                        "Kamera ruxsati berildi",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                Toast.makeText(
                        this,
                        "Kamera ruxsati berilmadi",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // =========================================================
    // BACK BUTTON
    // =========================================================

    private void setupBackButton() {

        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(
                                true
                        ) {

                            @Override
                            public void handleOnBackPressed() {

                                if (
                                        webView != null
                                        &&
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

    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        destroyed = false;

        if (webView != null) {

            handler.postDelayed(
                    this::syncSessionAndTracking,
                    1000
            );
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        destroyed = true;

        handler.removeCallbacksAndMessages(
                null
        );

        if (webView != null) {

            webView.stopLoading();

            webView.setWebChromeClient(
                    null
            );

            webView.setWebViewClient(
                    null
            );

            webView.destroy();

            webView = null;
        }

        super.onDestroy();
    }
}
