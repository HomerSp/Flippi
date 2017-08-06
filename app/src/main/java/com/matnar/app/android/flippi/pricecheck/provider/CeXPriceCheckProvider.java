package com.matnar.app.android.flippi.pricecheck.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.UrlQuerySanitizer;
import android.util.Log;

import com.matnar.app.android.flippi.db.PriceCheckDatabase;
import com.matnar.app.android.flippi.pricecheck.PriceCheckProvider;
import com.matnar.app.android.flippi.pricecheck.PriceCheckRegion;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.ref.WeakReference;

public class CeXPriceCheckProvider extends PriceCheckProvider {
    private static final String TAG = "Flippi." + CeXPriceCheckProvider.class.getSimpleName();

    private WeakReference<Context> mContext;
    private PriceCheckRegion.Region mRegion;

    public CeXPriceCheckProvider(WeakReference<Context> context, PriceCheckRegion.Region region) {
        mContext = context;
        mRegion = region;
    }

    @Override
    protected PriceCheckItems lookup(String name, int page, PriceCheckDatabase database, String filter) {
        SQLiteDatabase db = database.getWritableDatabase();

        try {
            Connection connection = Jsoup.connect(getSearchURL()).data("stext", name);
            if(page > 0) {
                connection.data("page", Integer.toString(page));
            }
            if(filter.length() > 0) {
                connection.data("refinecat", filter);
            }

            Document doc = connection.get();

            Elements searchRecords = doc.select("div.searchRcrd");
            if(searchRecords.size() == 1) {
                Element searchRecord = searchRecords.first();
                Elements rows = searchRecord.select("div.searchRecord");
                if(rows.size() < 1) {
                    // No results...
                    return new PriceCheckItems(1);
                }

                Elements searchAreas = doc.select("div.searchArea");
                if(searchAreas.size() != 1) {
                    throw(new RuntimeException("Could not find searchArea element!"));
                }

                Elements pageElements = searchAreas.first().select("div.paginationLinks > span.paginationPages > ul > li");
                if(pageElements.size() == 0) {
                    throw(new RuntimeException("Could not find pagination elements"));
                }

                PriceCheckItems info = new PriceCheckItems(pageElements.size());
                for(Element row: rows) {
                    PriceCheckItem i = infoFromSearchRow(row, db);
                    if(i == null) {
                        db.close();
                        throw(new RuntimeException("Could not obtain info from search row"));
                    }

                    info.add(i);
                }

                db.close();
                return info;
            } else {
                Elements productAreas = doc.select("div.productArea");
                if(productAreas.size() != 1) {
                    throw new RuntimeException("Could not find search or product area element");
                }

                Element productArea = productAreas.first();

                PriceCheckItems info = new PriceCheckItems(1);

                PriceCheckItem i = infoFromProductPage(productArea, db);
                if(i == null) {
                    throw(new RuntimeException("Could not obtain info from product page"));
                }

                db.close();
                info.add(i);
                return info;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving info", e);
        }

        return new PriceCheckItems(1, true);
    }

    private String getName() {
        return "cex";
    }

    private String getSearchURL() {
        switch(mRegion) {
            case RegionUK:
                return "https://uk.webuy.com/search/";
            case RegionUS:
                return "https://us.webuy.com/search/";
        }

        return "";
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
        if(prices.size() != 3) {
            return null;
        }

        String sellPrice = getRegionalPrice(prices.get(0));
        String buyPrice = getRegionalPrice(prices.get(1));
        String buyVoucherPrice = getRegionalPrice(prices.get(2));

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

        String sellPrice = getRegionalPrice(product.select("#Asellprice").first());
        String buyPrice = getRegionalPrice(product.select("#Acashprice").first());
        String buyVoucherPrice = getRegionalPrice(product.select("#Aexchprice").first());

        if(sellPrice.length() == 0 || buyPrice.length() == 0 || buyVoucherPrice.length() == 0) {
            return null;
        }

        return new PriceCheckItem(name, category, image, sku, sellPrice, buyPrice, buyVoucherPrice, getName(), mRegion, db);
    }

    private String getRegionalPrice(Element row) {
        if(row == null) {
            return "";
        }

        int index = -1;
        String rowText = row.text();
        if(rowText.length() == 0) {
            return "";
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
            return "";
        }

        return rowText.substring(index + 1);
    }
}
