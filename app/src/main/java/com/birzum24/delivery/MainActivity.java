package com.birzum24.delivery;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.annotation.NonNull;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://birzum.asakaedu.uz";
    private static final String START_URL = BASE_URL + "/c/";
    private static final String STATE_URL = BASE_URL + "/c/api.php?a=state";
    private static final String FCM_URL = BASE_URL + "/c/api.php?a=fcm_token";

    private static final int REQUEST_NOTIFICATION = 1001;
    private static final int REQUEST_LOCATION = 1002;
    private static final int REQUEST_CAMERA = 1003;
    private static final int REQUEST_BACKGROUND_LOCATION = 1004;

    private static final String PREFS = "birzum24_prefs";

    private WebView webView;
    private View rootLayout;

    private SharedPreferences prefs;

    private boolean permissionFlowRunning = false;
    private boolean destroyed = false;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarColor(android.graphics.Color.BLACK);
            getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
        }

        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        webView = findViewById(R.id.webView);

        if (rootLayout == null || webView == null) {
            Toast.makeText(
                    this,
                    "Ilova interfeysi topilmadi.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        setupSystemBars();
        setupWebView();
        setupBackButton();

        /*
         * Permissionlar birinchi kirishda ketma-ket so'raladi.
         * Ilovani qayta-qayta yopib ochishga hojat bo'lmaydi.
         */
        startPermissionFlow();

        /*
         * FCM tokenni olish.
         */
        getFcmToken();

        /*
         * Saytni ochamiz.
         */
        webView.loadUrl(START_URL);
    }

    // =========================================================
    // SYSTEM BAR / STATUS BAR
    // =========================================================

    private void setupSystemBars() {

        ViewCompat.setOnApplyWindowInsetsListener(
                rootLayout,
                (view, insets) -> {

                    androidx.core.graphics.Insets systemInsets =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            0,
                            systemInsets.top,
                            0,
                            systemInsets.bottom
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(rootLayout);
    }

    // =========================================================
    // WEBVIEW
    // =========================================================

    private void setupWebView() {

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setGeolocationEnabled(true);

        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);

        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(
                    WebSettings.MIXED_CONTENT_NEVER_ALLOW
            );
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(
                    webView,
                    true
            );
        }

        settings.setUserAgentString(
                settings.getUserAgentString()
                        + " BirZum24DeliveryAndroid/1.0"
        );

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url
            ) {
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

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

                    if (destroyed) {
                        request.deny();
                        return;
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            && checkSelfPermission(
                            Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED) {

                        request.deny();
                        return;
                    }

                    /*
                     * Faqat kamera ruxsatini beramiz.
                     * Mikrofon hozircha berilmaydi.
                     */
                    String[] resources = request.getResources();

                    boolean cameraRequested = false;

                    for (String resource : resources) {

                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                            cameraRequested = true;
                            break;
                        }
                    }

                    if (cameraRequested) {

                        request.grant(
                                new String[]{
                                        PermissionRequest.RESOURCE_VIDEO_CAPTURE
                                }
                        );

                    } else {

                        request.deny();
                    }
                });
            }
        });
    }

    // =========================================================
    // BACK BUTTON
    // =========================================================

    private void setupBackButton() {

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        if (webView != null && webView.canGoBack()) {

                            webView.goBack();

                        } else {

                            finish();
                        }
                    }
                }
        );
    }

    // =========================================================
    // PERMISSION FLOW
    // =========================================================

    private void startPermissionFlow() {

        if (permissionFlowRunning) {
            return;
        }

        permissionFlowRunning = true;

        handler.postDelayed(
                this::requestNextPermission,
                400
        );
    }

    private void requestNextPermission() {

        if (isFinishing() || isDestroyed()) {
            permissionFlowRunning = false;
            return;
        }

        /*
         * 1. Notification
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
                    && !prefs.getBoolean(
                    "notification_requested",
                    false
            )) {

                prefs.edit()
                        .putBoolean(
                                "notification_requested",
                                true
                        )
                        .apply();

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        REQUEST_NOTIFICATION
                );

                return;
            }
        }

        /*
         * 2. Foreground location
         */
        if (!hasLocationPermission()
                && !prefs.getBoolean(
                "location_requested",
                false
        )) {

            prefs.edit()
                    .putBoolean(
                            "location_requested",
                            true
                    )
                    .apply();

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION
            );

            return;
        }

        /*
         * 3. Camera
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
                    && !prefs.getBoolean(
                    "camera_requested",
                    false
            )) {

                prefs.edit()
                        .putBoolean(
                                "camera_requested",
                                true
                        )
                        .apply();

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.CAMERA
                        },
                        REQUEST_CAMERA
                );

                return;
            }
        }

        /*
         * 4. Background location.
         *
         * Android 11+ da buni foreground locationdan
         * alohida so'rash kerak.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            if (hasLocationPermission()
                    && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                    && !prefs.getBoolean(
                    "background_location_requested",
                    false
            )) {

                prefs.edit()
                        .putBoolean(
                                "background_location_requested",
                                true
                        )
                        .apply();

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        },
                        REQUEST_BACKGROUND_LOCATION
                );

                return;
            }
        }

        /*
         * Hamma kerakli permissionlar jarayoni tugadi.
         */
        permissionFlowRunning = false;

        startLocationServiceIfAllowed();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        /*
         * Har bir permissiondan keyin keyingisini
         * shu zahoti so'raymiz.
         */
        handler.postDelayed(
                this::requestNextPermission,
                300
        );
    }

    // =========================================================
    // LOCATION PERMISSION
    // =========================================================

    private boolean hasLocationPermission() {

        boolean fine =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarse =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        return fine || coarse;
    }

    private boolean hasBackgroundLocationPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true;
        }

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    // =========================================================
    // FOREGROUND LOCATION SERVICE
    // =========================================================

    private void startLocationServiceIfAllowed() {

        if (!hasLocationPermission()) {
            return;
        }

        try {

            Intent serviceIntent =
                    new Intent(
                            this,
                            LocationForegroundService.class
                    );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                ContextCompat.startForegroundService(
                        this,
                        serviceIntent
                );

            } else {

                startService(serviceIntent);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // FCM
    // =========================================================

    private void getFcmToken() {

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        return;
                    }

                    String token = task.getResult();

                    if (token == null || token.trim().isEmpty()) {
                        return;
                    }

                    saveFcmToken(token);
                });
    }

    private void saveFcmToken(String token) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(FCM_URL);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                String cookies =
                        CookieManager
                                .getInstance()
                                .getCookie(BASE_URL);

                if (cookies != null && !cookies.isEmpty()) {

                    connection.setRequestProperty(
                            "Cookie",
                            cookies
                    );
                }

                JSONObject json = new JSONObject();

                json.put(
                        "token",
                        token
                );

                byte[] body =
                        json.toString()
                                .getBytes(StandardCharsets.UTF_8);

                connection.getOutputStream().write(body);

                int responseCode =
                        connection.getResponseCode();

                InputStream stream;

                if (responseCode >= 200
                        && responseCode < 400) {

                    stream = connection.getInputStream();

                } else {

                    stream = connection.getErrorStream();
                }

                if (stream != null) {

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            stream,
                                            StandardCharsets.UTF_8
                                    )
                            );

                    StringBuilder result =
                            new StringBuilder();

                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    reader.close();

                    System.out.println(
                            "FCM SERVER: "
                                    + result
                    );
                }

                prefs.edit()
                        .putString(
                                "fcm_token",
                                token
                        )
                        .apply();

            } catch (Exception e) {

                e.printStackTrace();

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    // =========================================================
    // SESSION STATE
    // =========================================================

    private void syncSessionState() {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(STATE_URL);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                String cookies =
                        CookieManager
                                .getInstance()
                                .getCookie(BASE_URL);

                if (cookies != null && !cookies.isEmpty()) {

                    connection.setRequestProperty(
                            "Cookie",
                            cookies
                    );
                }

                connection.getResponseCode();

            } catch (Exception e) {

                e.printStackTrace();

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {
        super.onResume();

        if (destroyed) {
            return;
        }

        getFcmToken();
        syncSessionState();

        /*
         * App qayta ochilsa ham location service yana
         * ishga tushiriladi.
         */
        if (hasLocationPermission()) {
            startLocationServiceIfAllowed();
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        destroyed = true;

        if (webView != null) {

            webView.stopLoading();
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
