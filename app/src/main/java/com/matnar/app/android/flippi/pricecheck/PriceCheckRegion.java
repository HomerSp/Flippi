package com.matnar.app.android.flippi.pricecheck;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.DecimalFormat;

public class PriceCheckRegion {
    public enum Region {
        RegionUnknown,
        RegionUK,
        RegionUS
    }

    static Region getCurrent(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String region = prefs.getString("region", "uk");
        return fromString(region);
    }

    public static String getPrice(Region region, double price) {
        switch(region) {
            case RegionUK:
                return "£" + new DecimalFormat("0.00").format(price);
            case RegionUS:
                return "$" + new DecimalFormat("0.00").format(price);
        }

        return new DecimalFormat("0.00").format(price);
    }

    public static String getPrice(Context context, double price) {
        return getPrice(getCurrent(context), price);
    }

    public static Region fromString(String name) {
        if(name.equals("uk")) {
            return Region.RegionUK;
        } else if(name.equals("us")) {
            return Region.RegionUS;
        }

        return Region.RegionUnknown;
    }

    public static String toString(Region region) {
        switch(region) {
            case RegionUK:
                return "uk";
            case RegionUS:
                return "us";
        }

        return "";
    }
}
