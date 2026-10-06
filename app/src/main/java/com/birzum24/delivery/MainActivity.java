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
         * WebView status bar bilan ustma-ust tushmasin.
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

        /*
         * Local storage.
         */
        prefs = getSharedPreferences(
                "birzum_delivery",
                MODE_PRIVATE
        );

        setContentView(
                R.layout.activity_main
        );

        webView =
                findViewById(
                        R.id.webView
                );

        setupWebView();

        /*
         * Android permissionlar.
         */
        requestNotificationPermission();

        requestLocationPermission();

        /*
         * FCM token olish.
         */
        getFcmToken();

        /*
         * Android back tugmasi.
         */
        setupBackButton();

        /*
         * WebViewni ochish.
         */
        webView.loadUrl(URL);
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
         * Cookie/session.
         */
        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        if (
                Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.LOLLIPOP
        ) {

            cookieManager.setAcceptThirdPartyCookies(
                    webView,
                    true
            );
        }

        /*
         * WebViewClient.
         */
        webView.setWebViewClient(
                new WebViewClient() {

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
                         * Sessionni tekshiramiz.
                         */
                        syncSessionAndTracking();

                        /*
                         * WebView session biroz kechikib
                         * tiklanishi mumkin.
                         */
                        view.postDelayed(
                                MainActivity.this::
                                        syncSessionAndTracking,
                                3000
                        );
                    }
                }
        );

        /*
         * WebChromeClient.
         */
        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onGeolocationPermissionsShowPrompt(
                            String origin,
                            GeolocationPermissions.Callback callback
                    ) {

                        if (
                                hasLocationPermission()
                        ) {

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

                            for (
                                    String resource :
                                    resources
                            ) {

                                if (
                                        PermissionRequest
                                                .RESOURCE_VIDEO_CAPTURE
                                                .equals(resource)
                                ) {

                                    camera = true;

                                    break;
                                }
                            }

                            if (camera) {

                                if (
                                        ContextCompat.checkSelfPermission(
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

                            } else {

                                request.grant(
                                        resources
                                );
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

                            if (
                                    !task.isSuccessful()
                            ) {

                                String error;

                                if (
                                        task.getException()
                                                != null
                                ) {

                                    error =
                                            task.getException()
                                                    .getMessage();

                                } else {

                                    error =
                                            "Noma'lum Firebase xatosi";
                                }

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

                            /*
                             * Tokenni local saqlaymiz.
                             */
                            prefs.edit()
                                    .putString(
                                            "fcm_token",
                                            token
                                    )
                                    .apply();

                            Toast.makeText(
                                    MainActivity.this,
                                    "FCM TOKEN OLINDI",
                                    Toast.LENGTH_SHORT
                            ).show();

                            /*
                             * WebView session tayyor bo'lsa,
                             * PHPga yuboramiz.
                             */
                            syncSessionAndTracking();
                        }
                );
    }

    // =========================================================
    // SESSION + FCM + LOCATION
    // =========================================================

    private void syncSessionAndTracking() {

        if (webView == null) {
            return;
        }

        CookieManager cookieManager =
                CookieManager.getInstance();

        String cookie =
                cookieManager.getCookie(URL);

        if (
                cookie == null ||
                cookie.isEmpty()
        ) {

            return;
        }

        String js =
                "(async function(){"
                        + "try{"

                        + "const r=await fetch("
                        + "'/c/api.php?a=state',"
                        + "{"
                        + "method:'POST',"
                        + "credentials:'include',"
                        + "headers:{"
                        + "'Content-Type':'application/json'"
                        + "}"
                        + "}"
                        + ");"

                        + "const j=await r.json();"

                        + "return JSON.stringify(j);"

                        + "}catch(e){"

                        + "return JSON.stringify({"
                        + "ok:false,"
                        + "error:String(e)"
                        + "});"

                        + "}"

                        + "})()";

        webView.evaluateJavascript(
                js,
                result -> {

                    try {

                        if (
                                result == null ||
                                result.equals("null")
                        ) {

                            return;
                        }

                        String clean =
                                result;

                        /*
                         * evaluateJavascript
                         * qaytargan qo'shtirnoqlarni
                         * ochamiz.
                         */
                        if (
                                clean.startsWith("\"") &&
                                clean.endsWith("\"")
                        ) {

                            clean =
                                    clean.substring(
                                            1,
                                            clean.length() - 1
                                    );
                        }

                        clean =
                                clean
                                        .replace(
                                                "\\\"",
                                                "\""
                                        )
                                        .replace(
                                                "\\n",
                                                ""
                                        )
                                        .replace(
                                                "\\/",
                                                "/"
                                        )
                                        .replace(
                                                "\\\\",
                                                "\\"
                                        );

                        JSONObject data =
                                new JSONObject(
                                        clean
                                );

                        boolean ok =
                                data.optBoolean(
                                        "ok",
                                        false
                                );

                        if (!ok) {

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

                        if (
                                csrf.isEmpty()
                        ) {

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
                         * Faqat login tayyor bo'lganda
                         * tokenni PHPga yuboramiz.
                         */
                        if (
                                "ready".equals(stage)
                        ) {

                            syncFcmTokenToServer(
                                    csrf
                            );

                            /*
                             * Location service.
                             */
                            if (
                                    hasLocationPermission()
                            ) {

                                startLocationService();
                            }
                        }

                    } catch (Exception ignored) {

                    }
                }
        );
    }

    // =========================================================
    // FCM TOKEN -> PHP
    // =========================================================

    private void syncFcmTokenToServer(
            String csrf
    ) {

        String token =
                prefs.getString(
                        "fcm_token",
                        ""
                );

        if (
                token == null ||
                token.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "FCM token topilmadi",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            /*
             * JSON body.
             */
            JSONObject body =
                    new JSONObject();

            body.put(
                    "token",
                    token
            );

            String bodyJson =
                    body.toString();

            /*
             * JavaScript uchun quote.
             */
            String bodyEscaped =
                    JSONObject.quote(
                            bodyJson
                    );

            String csrfEscaped =
                    JSONObject.quote(
                            csrf
                    );

            /*
             * PHP APIga POST.
             */
            String js =
                    "(async function(){"
                            + "try{"

                            + "const r=await fetch("
                            + "'/c/api.php?a=fcm_token',"
                            + "{"

                            + "method:'POST',"

                            + "credentials:'include',"

                            + "headers:{"

                            + "'Content-Type':"
                            + "'application/json',"

                            + "'X-CSRF-Token':"
                            + csrfEscaped

                            + "},"

                            + "body:"
                            + bodyEscaped

                            + "}"
                            + ");"

                            + "const text=await r.text();"

                            + "return JSON.stringify({"

                            + "http:r.status,"

                            + "text:text"

                            + "});"

                            + "}catch(e){"

                            + "return JSON.stringify({"

                            + "error:String(e)"

                            + "});"

                            + "}"

                            + "})()";

            webView.evaluateJavascript(
                    js,
                    result -> {

                        try {

                            if (
                                    result == null ||
                                    result.equals("null")
                            ) {

                                Toast.makeText(
                                        this,
                                        "FCM server javobi yo‘q",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            String clean =
                                    result;

                            /*
                             * evaluateJavascript
                             * stringini ochamiz.
                             */
                            if (
                                    clean.startsWith("\"")
                                    &&
                                    clean.endsWith("\"")
                            ) {

                                clean =
                                        clean.substring(
                                                1,
                                                clean.length() - 1
                                        );
                            }

                            clean =
                                    clean
                                            .replace(
                                                    "\\\"",
                                                    "\""
                                            )
                                            .replace(
                                                    "\\n",
                                                    ""
                                            )
                                            .replace(
                                                    "\\/",
                                                    "/"
                                            )
                                            .replace(
                                                    "\\\\",
                                                    "\\"
                                            );

                            JSONObject response =
                                    new JSONObject(
                                            clean
                                    );

                            int http =
                                    response.optInt(
                                            "http",
                                            0
                                    );

                            String text =
                                    response.optString(
                                            "text",
                                            ""
                                    );

                            String error =
                                    response.optString(
                                            "error",
                                            ""
                                    );

                            /*
                             * JavaScript fetch xatosi.
                             */
                            if (
                                    !error.isEmpty()
                            ) {

                                Toast.makeText(
                                        this,
                                        "FCM API XATO:\n"
                                                + error,
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            /*
                             * PHP javobini ekranga chiqaramiz.
                             */
                            Toast.makeText(
                                    this,
                                    "FCM API HTTP "
                                            + http
                                            + "\n"
                                            + text,
                                    Toast.LENGTH_LONG
                            ).show();

                            /*
                             * Agar PHP:
                             * {"ok":true,"saved":1}
                             * qaytarsa, token saqlandi.
                             */
                            try {

                                JSONObject php =
                                        new JSONObject(
                                                text
                                        );

                                boolean saved =
                                        php.optBoolean(
                                                "ok",
                                                false
                                        );

                                prefs.edit()
                                        .putBoolean(
                                                "fcm_synced",
                                                saved
                                        )
                                        .apply();

                            } catch (
                                    Exception ignored
                            ) {

                            }

                        } catch (
                                Exception e
                        ) {

                            Toast.makeText(
                                    this,
                                    "FCM javobini o‘qishda xato:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

        } catch (
                Exception e
        ) {

            Toast.makeText(
                    this,
                    "FCM yuborishda xato:\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
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

        if (
                hasLocationPermission()
        ) {

            /*
             * Android 10+ background location.
             */
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
                        "BirZum24 kuryerlar uchun buyurtma vaqtida joylashuvingizni fonda ham yuborishi kerak. Keyingi oynada \"Har doim ruxsat berish\"ni tanlang."
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
    // NOTIFICATION PERMISSION
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
    // CAMERA PERMISSION
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

        } catch (
                Exception e
        ) {

            Toast.makeText(
                    this,
                    "Location service xatosi:\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // PERMISSION RESULT
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

            if (
                    hasLocationPermission()
            ) {

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

            if (
                    Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.Q
            ) {

                if (
                        ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                        ==
                        PackageManager.PERMISSION_GRANTED
                ) {

                    Toast.makeText(
                            this,
                            "Fonda joylashuv ruxsati berildi",
                            Toast.LENGTH_SHORT
                    ).show();

                } else {

                    Toast.makeText(
                            this,
                            "Fonda joylashuv ruxsati berilmadi",
                            Toast.LENGTH_LONG
                    ).show();
                }
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
    // ACTIVITY RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        /*
         * Ilova qayta ochilganda sessionni
         * qayta tekshiramiz.
         */
        if (webView != null) {

            webView.postDelayed(
                    this::syncSessionAndTracking,
                    500
            );
        }
    }

    // =========================================================
    // ACTIVITY DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

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
