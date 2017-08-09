package com.matnar.app.android.flippi.pricecheck;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.matnar.app.android.flippi.db.PriceCheckDatabase;
import com.matnar.app.android.flippi.pricecheck.provider.CeXPriceCheckProvider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public abstract class PriceCheckProvider {
    private static final String TAG = "Flippi." + PriceCheckProvider.class.getSimpleName();

    public static void getInformation(WeakReference<Context> context, String query, PriceCheckDatabase database, String filter, String sort, PriceCheckListener listener) {
        getInformation(context, query, 0, database, filter, sort, listener);
    }

    public static void getInformation(WeakReference<Context> context, String query, int page, PriceCheckDatabase database, String filter, String sort, PriceCheckListener listener) {
        new PriceCheckTask(context, page, database, filter, sort, listener).execute(query);
    }

    protected abstract PriceCheckItems lookup(String name, int page, PriceCheckDatabase database, String filter, String sort);

    public interface PriceCheckListener {
        void onResult(PriceCheckItems result);
    }

    public static final class PriceCheckItems extends ArrayList<PriceCheckItem> implements Parcelable {
        private boolean mHasError = false;
        private int mPages = 0;

        public PriceCheckItems() {
            this(1, false);
        }

        public PriceCheckItems(int pages) {
            this(pages, false);
        }

        public PriceCheckItems(int pages, boolean hasError) {
            mPages = pages;
            mHasError = hasError;
        }

        public int getPages() {
            return mPages;
        }

        public boolean hasError() {
            return mHasError;
        }

        public void setHasError() {
            mHasError = true;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt((mHasError) ? 1 : 0);
            dest.writeInt(mPages);
            dest.writeInt(size());
            for(PriceCheckItem item: this) {
                item.writeToParcel(dest, flags);
            }
        }

        public void addAll(Parcelable other) {
            addAll((PriceCheckItems) other);
        }

        public void addAll(PriceCheckItems other) {
            mHasError = other.mHasError;
            mPages = other.mPages;
            for(Parcelable p: other) {
                add((PriceCheckItem) p);
            }
        }

        public void sort(final String type) {
            Collections.sort(this, new Comparator<PriceCheckItem>() {
                public int compare(PriceCheckItem a, PriceCheckItem b) {
                    if(a == null || b == null) {
                        return 0;
                    }

                    if(type != null) {
                        switch (type) {
                            case "date_desc":
                                return -a.getDate().compareTo(b.getDate());
                            case "date_asc":
                                return a.getDate().compareTo(b.getDate());
                            case "sellprice_asc":
                                return Double.valueOf(a.getSellPrice()).compareTo(b.getSellPrice());
                            case "sellprice_desc":
                                return Double.valueOf(b.getSellPrice()).compareTo(a.getSellPrice());
                            case "name_az":
                                return a.getName().compareTo(b.getName());
                            case "name_za":
                                return -a.getName().compareTo(b.getName());
                        }
                    }

                    return -a.getDate().compareTo(b.getDate());
                }
            });
        }

        PriceCheckItems(Parcel in) {
            mHasError = in.readInt() == 1;
            mPages = in.readInt();
            int size = in.readInt();
            for(int i = 0; i < size; i++) {
                add(new PriceCheckItem(in));
            }
        }

        public static final Parcelable.Creator<PriceCheckItems> CREATOR = new Parcelable.Creator<PriceCheckItems>() {
            public PriceCheckItems createFromParcel(Parcel source) {
                return new PriceCheckItems(source);
            }

            public PriceCheckItems[] newArray(int size) {
                return new PriceCheckItems[size];
            }
        };
    }

    public static class PriceCheckItem implements Parcelable {
        private String mName;
        private String mCategory;
        private String mImage;
        private String mSKU;

        private double mSellPrice;
        private double mBuyPrice;
        private double mBuyVoucherPrice;

        private String mProvider;
        private PriceCheckRegion.Region mRegion;
        private Date mDate;

        private boolean mSaved = true;

        public PriceCheckItem(String name, String category, String image, String sku, double sellPrice, double buyPrice, double buyVoucherPrice, String provider, PriceCheckRegion.Region region) {
            this(name, category, image, sku, sellPrice, buyPrice, buyVoucherPrice, provider, region, (Date) null);
        }

        public PriceCheckItem(String name, String category, String image, String sku, double sellPrice, double buyPrice, double buyVoucherPrice, String provider, PriceCheckRegion.Region region, Date date) {
            mName = name;
            mCategory = category;
            mSKU = sku;
            mImage = image;
            mSellPrice = sellPrice;
            mBuyPrice = buyPrice;
            mBuyVoucherPrice = buyVoucherPrice;
            mProvider = provider;
            mRegion = region;
            mDate = date;
        }

        public PriceCheckItem(String name, String category, String image, String sku, double sellPrice, double buyPrice, double buyVoucherPrice, String provider, PriceCheckRegion.Region region, SQLiteDatabase db) {
            this(name, category, image, sku, sellPrice, buyPrice, buyVoucherPrice, provider, region, db, null);
        }

        public PriceCheckItem(String name, String category, String image, String sku, double sellPrice, double buyPrice, double buyVoucherPrice, String provider, PriceCheckRegion.Region region, SQLiteDatabase db, Date date) {
            this(name, category, image, sku, sellPrice, buyPrice, buyVoucherPrice, provider, region, date);

            mSaved = PriceCheckDatabase.exists(db, this, 0);
        }

        public String getProvider() {
            return mProvider;
        }

        public PriceCheckRegion.Region getRegion() {
            return mRegion;
        }

        public String getName() {
            return mName;
        }

        public String getCategory() {
            return mCategory;
        }

        public String getImage() {
            return mImage;
        }

        public String getSKU() {
            return mSKU;
        }

        public double getSellPrice() {
            return mSellPrice;
        }

        public double getBuyPrice() {
            return mBuyPrice;
        }

        public double getBuyVoucherPrice() {
            return mBuyVoucherPrice;
        }

        public Date getDate() {
            return mDate;
        }

        public boolean isSaved() {
            return mSaved;
        }

        public void setSaved(boolean b) {
            mSaved = b;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mName);
            dest.writeString(mCategory);
            dest.writeString(mImage);
            dest.writeString(mSKU);
            dest.writeDouble(mSellPrice);
            dest.writeDouble(mBuyPrice);
            dest.writeDouble(mBuyVoucherPrice);
            dest.writeString(mProvider);
            dest.writeString(PriceCheckRegion.toString(mRegion));
        }

        PriceCheckItem(Parcel in) {
            mName = in.readString();
            mCategory = in.readString();
            mImage = in.readString();
            mSKU = in.readString();
            mSellPrice = in.readDouble();
            mBuyPrice = in.readDouble();
            mBuyVoucherPrice = in.readDouble();
            mProvider = in.readString();
            mRegion = PriceCheckRegion.fromString(in.readString());
        }

        public static final Parcelable.Creator<PriceCheckItem> CREATOR = new Parcelable.Creator<PriceCheckItem>() {
            public PriceCheckItem createFromParcel(Parcel source) {
                return new PriceCheckItem(source);
            }

            public PriceCheckItem[] newArray(int size) {
                return new PriceCheckItem[size];
            }
        };
    }

    public static final class AdItem extends PriceCheckItem {
        public AdItem() {
            super("", "", "", "", 0.0f, 0.0f, 0.0f, "", PriceCheckRegion.Region.RegionUnknown);
        }
    }

    private static class PriceCheckTask extends AsyncTask<String, Void, PriceCheckItems> {
        private WeakReference<Context> mContext;
        private int mPage = 0;
        private PriceCheckDatabase mDatabase;
        private String mFilter;
        private String mSort;
        private PriceCheckListener mListener;
        private PriceCheckRegion.Region mRegion;

        PriceCheckTask(WeakReference<Context> context, int page, PriceCheckDatabase database, String filter, String sort, PriceCheckListener listener) {
            mContext = context;
            mPage = page;
            mDatabase = database;
            mFilter = filter;
            mSort = sort;
            mListener = listener;
            mRegion = PriceCheckRegion.getCurrent(context.get());
        }


        @Override
        protected PriceCheckItems doInBackground(String... query) {
            if(query.length == 0) {
                return null;
            }

            PriceCheckProvider provider = new CeXPriceCheckProvider(mContext, mRegion);
            return provider.lookup(query[0], mPage, mDatabase, mFilter, mSort);
        }

        @Override
        protected void onPostExecute(PriceCheckItems info) {
            if(mListener != null) {
                mListener.onResult(info);
            }
        }
    }
}
