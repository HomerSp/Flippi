package com.matnar.app.android.flippi.pricecheck.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.util.JsonReader;
import android.util.Log;

import com.matnar.app.android.flippi.db.PriceCheckDatabase;
import com.matnar.app.android.flippi.pricecheck.PriceCheckProvider;
import com.matnar.app.android.flippi.pricecheck.PriceCheckRegion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.ref.WeakReference;

public class CeXPriceCheckProvider extends PriceCheckProvider {
    private static final String TAG = "Flippi." + CeXPriceCheckProvider.class.getSimpleName();

    private static final int PAGE_COUNT = 20;

    private WeakReference<Context> mContext;
    private PriceCheckRegion.Region mRegion;

    public CeXPriceCheckProvider(WeakReference<Context> context, PriceCheckRegion.Region region) {
        mContext = context;
        mRegion = region;
    }

    @Override
    protected PriceCheckItems lookup(String name, int page, PriceCheckDatabase database, String filter, long filterCategory, String sort) {
        SQLiteDatabase db = database.getWritableDatabase();

        try {
            Connection connection = Jsoup.connect(getSearchURL());
            connection.ignoreContentType(true);
            connection.method(Connection.Method.GET);
            connection.header("Referer", getRefererURL());

            connection.data("count", Integer.toString(PAGE_COUNT));
            connection.data("firstRecord", Integer.toString(PAGE_COUNT * page));
            if(name != null) {
                connection.data("q", encode(name));
            }
            if(filterCategory != 0) {
                connection.data("categoryId", Long.toString(filterCategory));
            }
            if(sort != null) {
                if(sort.endsWith("-asc")) {
                    connection.data("sortBy", encode(sort.substring(0, sort.length() - 4)));
                    connection.data("sortOrder", "asc");
                } else if(sort.endsWith("-desc")) {
                    connection.data("sortBy", encode(sort.substring(0, sort.length() - 5)));
                    connection.data("sortOrder", "desc");
                } else {
                    connection.data("sortBy", encode(sort));
                    connection.data("sortOrder", "desc");
                }
            } else {
                connection.data("sortBy", "relevance");
                connection.data("sortOrder", "desc");
            }

            Connection.Response resp = connection.execute();
            try {
                JSONObject responseObj = new JSONObject(resp.body()).getJSONObject("response");
                if(responseObj.isNull("data")) {
                    return new PriceCheckItems(1);
                }
                JSONObject dataObj = responseObj.getJSONObject("data");
                JSONArray boxesArr = dataObj.getJSONArray("boxes");

                PriceCheckItems info = new PriceCheckItems((int) Math.ceil(dataObj.getLong("totalRecords") / PAGE_COUNT));
                for(int i = 0; i < boxesArr.length(); i++) {
                    try {
                        PriceCheckItem item = infoFromBoxRow(boxesArr.getJSONObject(i), db);
                        info.add(item);
                    } catch(JSONException e) {
                        Log.e(TAG, "Could not retrieve JSON data from row", e);
                    }
                }

                db.close();
                return info;
            } catch(JSONException e) {
                Log.e(TAG, "Could not retrieve JSON data", e);

                throw(new RuntimeException("Could not obtain info from product page"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving info", e);
        }

        return new PriceCheckItems(1, true);
    }

    private String encode(String e) {
        e = Uri.encode(e);
        return e.replaceAll("%20", "+");
    }

    private String getName() {
        return "cex";
    }

    private String getSearchURL() {
        switch(mRegion) {
            case RegionUK:
                return "https://wss2.cex.uk.webuy.io/v3/boxes";
            case RegionUS:
                return "https://wss2.cex.us.webuy.io/v3/boxes";
        }

        return "";
    }

    private String getRefererURL() {
        switch(mRegion) {
            case RegionUK:
                return "https://uk.webuy.com";
            case RegionUS:
                return "https://us.webuy.com";
        }

        return "";
    }

    private PriceCheckItem infoFromBoxRow(JSONObject boxRow, SQLiteDatabase db) throws JSONException {
        String name = boxRow.getString("boxName");
        String category = boxRow.getString("superCatFriendlyName") + " > " + boxRow.getString("categoryFriendlyName");
        String image = boxRow.getJSONObject("imageUrls").getString("large");
        String sku = boxRow.getString("boxId");
        double sellPrice = boxRow.getDouble("sellPrice");
        double buyPrice = boxRow.getDouble("cashPrice");
        double buyVoucherPrice = boxRow.getDouble("exchangePrice");

        return new PriceCheckItem(name, category, image, sku, sellPrice, buyPrice, buyVoucherPrice, getName(), mRegion, db);
    }

    private PriceCheckItem infoFromSearchRow(Element row, SQLiteDatabase db) {
        String name = row.select("div.desc > h1 > a").text();
        String category = row.select("div.desc > p").text();
        String image = row.select("div.thumb > a > img").attr("abs:src");
        image = image.replaceAll("_l.jpg$", "_s.jpg");

        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
        sanitizer.setAllowUnregisteredParamaters(true);
        sanitizer.parseUrl(row.select("div.thumb > a").attr("abs:href"));
        String sku = sanitizer.getValue("sku");

        Elements prices = row.select("div.desc > div.prodPrice div.priceTxt");

        double sellPrice = 0.0f;
        double buyPrice = 0.0f;
        double buyVoucherPrice = 0.0f;

        try {
            sellPrice = getRegionalPrice(prices.get(0));
            buyPrice = getRegionalPrice(prices.get(1));
            buyVoucherPrice = getRegionalPrice(prices.get(2));
        } catch(IndexOutOfBoundsException e) {
            // Empty
        }

        return new PriceCheckItem(name, category, image, sku, sellPrice, buyPrice, buyVoucherPrice, getName(), mRegion, db);
    }

    private PriceCheckItem infoFromProductPage(Element product, SQLiteDatabase db) {
        String name = product.select("div.productDetails div.productNamecustm").text();
        String category = product.select("div.superCatLink").text();
        String image = product.select("div.productInfoImageArea > div.productImg > img").attr("abs:src");
        image = image.replaceAll("_l.jpg$", "_s.jpg");

        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
        sanitizer.setAllowUnregisteredParamaters(true);
        sanitizer.parseUrl(product.select("div.productDetails div.btnSection > a").first().attr("abs:href"));
        String sku = sanitizer.getValue("id");

        double sellPrice = getRegionalPrice(product.select("#Asellprice").first());
        double buyPrice = getRegionalPrice(product.select("#Acashprice").first());
        double buyVoucherPrice = getRegionalPrice(product.select("#Aexchprice").first());

        return new PriceCheckItem(name, category, image, sku, sellPrice, buyPrice, buyVoucherPrice, getName(), mRegion, db);
    }

    private double getRegionalPrice(Element row) {
        if(row == null) {
            return 0.0f;
        }

        int index = -1;
        String rowText = row.text();
        if(rowText.length() == 0) {
            return 0.0f;
        }

        switch(mRegion) {
            case RegionUK:
                index = rowText.lastIndexOf('£');
                break;
            case RegionUS:
                index = rowText.lastIndexOf('$');
                break;

        }

        if(index < 0 || index >= rowText.length()) {
            return 0.0f;
        }

        return Double.parseDouble(rowText.substring(index + 1));
    }
}
