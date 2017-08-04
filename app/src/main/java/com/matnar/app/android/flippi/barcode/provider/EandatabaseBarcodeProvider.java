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

public class EandatabaseBarcodeProvider extends BarcodeProvider {
    private static final String TAG = "Flippi." + EandatabaseBarcodeProvider.class.getSimpleName();

    private WeakReference<Context> mContext;

    public EandatabaseBarcodeProvider(WeakReference<Context> context) {
        mContext = context;
    }

    @Override
    protected BarcodeInformation lookup(String barcode) {
        try {
            Connection connection = Jsoup.connect("http://ean-database.info/").data("q", barcode);
            Document doc = connection.get();

            Elements aRecords = doc.select("div.row > div > p > a");
            if(aRecords.size() > 1) {
                String title = aRecords.first().text();
                return new BarcodeInformation(title);
            }
        } catch (IOException e) {
            Log.e(TAG, "Barcode lookup error", e);
        }

        return null;
    }
}
