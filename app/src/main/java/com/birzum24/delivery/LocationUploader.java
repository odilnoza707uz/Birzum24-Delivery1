package com.birzum24.delivery;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LocationUploader {

    private static final String TAG = "BirZumLocation";

    private static final String API_URL =
            "https://birzum.asakaedu.uz/c/api.php?a=loc";


    public static boolean send(
            double latitude,
            double longitude,
            String cookie,
            String csrf
    ) {

        HttpURLConnection connection = null;

        try {

            URL url = new URL(API_URL);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            connection.setConnectTimeout(15000);

            connection.setReadTimeout(15000);

            connection.setDoInput(true);

            connection.setDoOutput(true);


            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );


            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );


            if (cookie != null && !cookie.isEmpty()) {

                connection.setRequestProperty(
                        "Cookie",
                        cookie
                );
            }


            if (csrf != null && !csrf.isEmpty()) {

                connection.setRequestProperty(
                        "X-CSRF-TOKEN",
                        csrf
                );
            }


            JSONObject json = new JSONObject();

            json.put("lat", latitude);

            json.put("lng", longitude);


            byte[] body =
                    json.toString()
                            .getBytes(StandardCharsets.UTF_8);


            try (OutputStream output =
                         connection.getOutputStream()) {

                output.write(body);

                output.flush();
            }


            int code =
                    connection.getResponseCode();


            InputStream stream;

            if (code >= 200 && code < 400) {

                stream =
                        connection.getInputStream();

            } else {

                stream =
                        connection.getErrorStream();
            }


            if (stream != null) {

                StringBuilder response =
                        new StringBuilder();

                try (BufferedReader reader =
                             new BufferedReader(
                                     new InputStreamReader(
                                             stream,
                                             StandardCharsets.UTF_8
                                     )
                             )) {

                    String line;

                    while ((line = reader.readLine()) != null) {

                        response.append(line);
                    }
                }

                Log.d(
                        TAG,
                        "HTTP " + code +
                                " response=" +
                                response
                );
            }


            return code >= 200 && code < 300;

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Location upload failed",
                    e
            );

            return false;

        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
    }
}
