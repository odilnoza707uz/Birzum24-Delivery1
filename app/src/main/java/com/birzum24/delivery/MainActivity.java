package com.birzum24.delivery;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String URL =
            "https://birzum.asakaedu.uz/c/";

    private static final int LOCATION_REQUEST = 1001;
    private static final int NOTIFICATION_REQUEST = 1002;
    private static final int CAMERA_REQUEST = 1003;
    private static final int BACKGROUND_LOCATION_REQUEST = 1011;

    private WebView webView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * WebView status bar ostiga kirib ketmasligi uchun.
         */
        WindowCompat.setDecorFitsSystemWindows(
                getWindow(),
                true
        );

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        prefs = getSharedPreferences(
                "birzum_delivery",
                MODE_PRIVATE
        );

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);

        setupWebView();

        requestNotificationPermission();
        requestLocationPermission();

        /*
         * FCM tokenni olish.
         */
        getFcmToken();

        setupBackButton();

        webView.loadUrl(URL);
    }

    private void setupWebView() {

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);

        settings.setMediaPlaybackRequiresUserGesture(false);

        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);

        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(
                    webView,
                    true
            );
        }

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {

                        super.onPageFinished(view, url);

                        syncSessionAndTracking();

                        view.postDelayed(
                                MainActivity.this::syncSessionAndTracking,
                                3000
                        );
                    }
                }
        );

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

                            boolean camera = false;

                            for (String resource : resources) {

                                if (PermissionRequest
                                        .RESOURCE_VIDEO_CAPTURE
                                        .equals(resource)) {

                                    camera = true;
                                    break;
                                }
                            }

                            if (camera) {

                                if (ContextCompat.checkSelfPermission(
                                        MainActivity.this,
                                        Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED) {

                                    request.grant(resources);

                                } else {

                                    requestCameraPermission();
                                }

                            } else {

                                request.grant(resources);
                            }
                        });
                    }
                }
        );
    }

    /*
     * Firebase FCM tokenini olamiz.
     */
    private void getFcmToken() {

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        return;
                    }

                    String token = task.getResult();

                    if (token == null || token.trim().isEmpty()) {
                        return;
                    }

                    prefs.edit()
                            .putString("fcm_token", token)
                            .apply();

                    /*
                     * Agar WebView allaqachon login bo'lgan bo'lsa,
                     * tokenni serverga yuborishga harakat qilamiz.
                     */
                    syncSessionAndTracking();
                });
    }

    /*
     * WebView'dan PHP session cookie +
     * CSRF tokenni olamiz.
     */
    private void syncSessionAndTracking() {

        if (webView == null) {
            return;
        }

        CookieManager cookieManager =
                CookieManager.getInstance();

        String cookie =
                cookieManager.getCookie(URL);

        if (cookie == null || cookie.isEmpty()) {
            return;
        }

        String js =
                "(async function(){"
                        + "try{"
                        + "const r=await fetch('/c/api.php?a=state',{"
                        + "method:'POST',"
                        + "credentials:'include',"
                        + "headers:{'Content-Type':'application/json'}"
                        + "});"
                        + "const j=await r.json();"
                        + "return JSON.stringify(j);"
                        + "}catch(e){"
                        + "return JSON.stringify({ok:false,error:String(e)});"
                        + "}"
                        + "})()";

        webView.evaluateJavascript(
                js,
                result -> {

                    try {

                        if (result == null ||
                                result.equals("null")) {
                            return;
                        }

                        String clean = result;

                        if (clean.startsWith("\"")
                                && clean.endsWith("\"")) {

                            clean = clean.substring(
                                    1,
                                    clean.length() - 1
                            );
                        }

                        clean = clean
                                .replace("\\\"", "\"")
                                .replace("\\n", "")
                                .replace("\\/", "/")
                                .replace("\\\\", "\\");

                        JSONObject data =
                                new JSONObject(clean);

                        if (!data.optBoolean("ok", false)) {
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

                        if (csrf.isEmpty()) {
                            return;
                        }

                        prefs.edit()
                                .putString(
                                        "cookie",
                                        cookie
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
                         * FCM tokenni faqat courier ready
                         * bo'lganda serverga yuboramiz.
                         */
                        if ("ready".equals(stage)) {

                            syncFcmTokenToServer(csrf);

                            if (hasLocationPermission()) {
                                startLocationService();
                            }
                        }

                    } catch (Exception e) {

                        /*
                         * JSON xatosi trackingni
                         * yiqitmasin.
                         */
                    }
                }
        );
    }

    /*
     * FCM tokenni PHP API'ga yuborish.
     *
     * /c/api.php?a=fcm_token
     *
     * Bu endpointni keyingi bosqichda PHP'ga qo'shamiz.
     */
    private void syncFcmTokenToServer(String csrf) {

        String token =
                prefs.getString("fcm_token", "");

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        try {

            JSONObject body =
                    new JSONObject();

            body.put("token", token);

            String bodyJson =
                    body.toString();

            /*
             * Java string ichida JS uchun
             * xavfsiz JSON.
             */
            String escaped =
                    JSONObject.quote(bodyJson);

            String csrfEscaped =
                    JSONObject.quote(csrf);

            String js =
                    "(async function(){"
                            + "try{"
                            + "const r=await fetch('/c/api.php?a=fcm_token',{"
                            + "method:'POST',"
                            + "credentials:'include',"
                            + "headers:{"
                            + "'Content-Type':'application/json',"
                            + "'X-CSRF-Token':" + csrfEscaped
                            + "},"
                            + "body:" + escaped
                            + "});"
                            + "const j=await r.json();"
                            + "return JSON.stringify(j);"
                            + "}catch(e){"
                            + "return JSON.stringify({ok:false,error:String(e)});"
                            + "}"
                            + "})()";

            webView.evaluateJavascript(
                    js,
                    result -> {
                        /*
                         * Hozircha javobni ko'rsatmaymiz.
                         */
                    }
            );

        } catch (Exception e) {

            /*
             * FCM xatosi ilovani yiqitmasin.
             */
        }
    }

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

    private boolean hasFineLocationPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!hasLocationPermission()) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_REQUEST
                );
            }
        }
    }

    private void requestBackgroundLocationPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            new AlertDialog.Builder(this)
                    .setTitle(
                            "Fonda joylashuvga ruxsat"
                    )
                    .setMessage(
                            "BirZum24 Delivery kuryer buyurtmasini yetkazayotgan paytda ilova yopiq yoki ekran o‘chiq bo‘lsa ham joylashuvni yuborishi kerak."
                    )
                    .setPositiveButton(
                            "Sozlamalarni ochish",
                            (dialog, which) -> {

                                try {

                                    Intent intent =
                                            new Intent(
                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                            );

                                    intent.setData(
                                            Uri.parse(
                                                    "package:"
                                                            + getPackageName()
                                            )
                                    );

                                    startActivity(intent);

                                } catch (Exception e) {

                                    Intent intent =
                                            new Intent(
                                                    Settings.ACTION_SETTINGS
                                            );

                                    startActivity(intent);
                                }
                            }
                    )
                    .setNegativeButton(
                            "Keyinroq",
                            null
                    )
                    .show();

        } else {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        },
                        BACKGROUND_LOCATION_REQUEST
                );
            }
        }
    }

    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        NOTIFICATION_REQUEST
                );
            }
        }
    }

    private void requestCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.CAMERA
                    },
                    CAMERA_REQUEST
            );
        }
    }

    private void startLocationService() {

        Intent intent =
                new Intent(
                        this,
                        LocationForegroundService.class
                );

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            startForegroundService(intent);

        } else {

            startService(intent);
        }
    }

    private void stopLocationService() {

        Intent intent =
                new Intent(
                        this,
                        LocationForegroundService.class
                );

        stopService(intent);
    }

    private void setupBackButton() {

        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {

                            @Override
                            public void handleOnBackPressed() {

                                if (webView != null
                                        && webView.canGoBack()) {

                                    webView.goBack();

                                } else {

                                    finish();
                                }
                            }
                        }
                );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
       
            }
        }
