package com.matnar.app.android.flippi.barcode.provider;

import android.content.Context;

import com.matnar.app.android.flippi.barcode.BarcodeProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class OutpanBarcodeProvider extends BarcodeProvider {
    private static final String API_KEY = "8d4280e71e816efb4ad0edc9b8f25b2d";

    private WeakReference<Context> mContext;

    public OutpanBarcodeProvider(WeakReference<Context> context) {
        mContext = context;
    }

    @Override
    protected BarcodeInformation lookup(String barcode) {
        URL url;
        HttpsURLConnection connection = null;

        try {
            url = new URL("https://api.outpan.com/v2/products/" + barcode + "?apikey=" + API_KEY);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = reader.readLine()) != null) {
                responseStrBuilder.append(inputStr);
            }

            JSONObject obj = new JSONObject(responseStrBuilder.toString());

            if(obj.has("name") && !obj.isNull("name")) {
                String name = obj.getString("name");
                return new BarcodeInformation(super.format(name));
            }
        } catch (java.io.IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }
}
