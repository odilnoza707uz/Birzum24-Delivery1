package com.birzum24.delivery;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.messaging.FirebaseMessagingService;

public class BirZumFirebaseMessagingService
        extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences(
                        "birzum_delivery",
                        Context.MODE_PRIVATE
                );

        prefs.edit()
                .putString("fcm_token", token)
                .apply();
    }
        }
