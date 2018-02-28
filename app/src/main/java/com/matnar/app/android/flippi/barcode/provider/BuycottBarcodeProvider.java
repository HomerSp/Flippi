package com.matnar.app.android.flippi.barcode.provider;

import android.content.Context;
import android.util.Log;

import com.matnar.app.android.flippi.barcode.BarcodeProvider;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class BuycottBarcodeProvider extends BarcodeProvider {
    private static final String TAG = "Flippi." + BuycottBarcodeProvider.class.getSimpleName();

    private WeakReference<Context> mContext;

    public BuycottBarcodeProvider(WeakReference<Context> context) {
        mContext = context;
    }

    @Override
    protected BarcodeInformation lookup(String barcode) {
        try {
            Connection connection = Jsoup.connect("https://buycott.com/upc/" + barcode);
            Document doc = connection.get();

            Elements containerRecords = doc.select("div#container_header");
            if(containerRecords.size() == 1) {
                String title = containerRecords.first().select("> h2").text();
                return new BarcodeInformation(title);
            }
        } catch (IOException e) {
            Log.e(TAG, "lookup", e);
        }

        return null;
    }
}

