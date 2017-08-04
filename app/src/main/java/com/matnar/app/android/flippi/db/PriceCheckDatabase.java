package com.matnar.app.android.flippi.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.util.Log;

import com.matnar.app.android.flippi.pricecheck.PriceCheckProvider;
import com.matnar.app.android.flippi.pricecheck.PriceCheckRegion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PriceCheckDatabase extends SQLiteOpenHelper {
    private static final String TAG = "Flippi." + PriceCheckDatabase.class.getSimpleName();

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "data.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + PriceEntry.TABLE_NAME + " (" +
                    PriceEntry._ID + " INTEGER PRIMARY KEY," +
                    PriceEntry.COLUMN_NAME_BUNDLE + " INTEGER," +
                    PriceEntry.COLUMN_NAME_PROVIDER + " TEXT," +
                    PriceEntry.COLUMN_NAME_REGION + " TEXT," +
                    PriceEntry.COLUMN_NAME_NAME + " TEXT," +
                    PriceEntry.COLUMN_NAME_IMAGE + " TEXT," +
                    PriceEntry.COLUMN_NAME_SKU + " TEXT," +
                    PriceEntry.COLUMN_NAME_CATEGORY + " TEXT," +
                    PriceEntry.COLUMN_NAME_SELLPRICE + " TEXT," +
                    PriceEntry.COLUMN_NAME_CASHPRICE + " TEXT," +
                    PriceEntry.COLUMN_NAME_VOUCHERPRICE + " TEXT," +
                    PriceEntry.COLUMN_NAME_DATE + " INTEGER" +
                    ")";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + PriceEntry.TABLE_NAME;

    public PriceCheckDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private static boolean clear(SQLiteDatabase db, int bundle) {
        String selection = PriceEntry.COLUMN_NAME_BUNDLE + " = ?";
        String[] selectionArgs = {
                Integer.toString(bundle)
        };

        return db.delete(PriceEntry.TABLE_NAME, selection, selectionArgs) > 0;
    }

    private static List<PriceCheckProvider.PriceCheckItem> getAll(SQLiteDatabase db, int bundle) {
        List<PriceCheckProvider.PriceCheckItem> ret = new ArrayList<>();

        String[] projection = {
                PriceEntry._ID,
                PriceEntry.COLUMN_NAME_BUNDLE,
                PriceEntry.COLUMN_NAME_PROVIDER,
                PriceEntry.COLUMN_NAME_REGION,
                PriceEntry.COLUMN_NAME_NAME,
                PriceEntry.COLUMN_NAME_IMAGE,
                PriceEntry.COLUMN_NAME_SKU,
                PriceEntry.COLUMN_NAME_CATEGORY,
                PriceEntry.COLUMN_NAME_SELLPRICE,
                PriceEntry.COLUMN_NAME_CASHPRICE,
                PriceEntry.COLUMN_NAME_VOUCHERPRICE,
                PriceEntry.COLUMN_NAME_DATE
        };

        String sortOrder =
                PriceEntry.COLUMN_NAME_DATE + " DESC";

        Cursor cursor;

        if(bundle > 0) {
            String selection = PriceEntry.COLUMN_NAME_BUNDLE + " = ?";
            String[] selectionArgs = {
                    Integer.toString(bundle)
            };

            cursor = db.query(
                    PriceEntry.TABLE_NAME,                     // The table to query
                    projection,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    sortOrder                                 // The sort order
            );
        } else {
            cursor = db.query(
                    PriceEntry.TABLE_NAME,                     // The table to query
                    projection,                               // The columns to return
                    null,                                // The columns for the WHERE clause
                    null,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    sortOrder                                 // The sort order
            );
        }

        while(cursor.moveToNext()) {
            try {
                long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(PriceEntry._ID));
                int itemBundle = cursor.getInt(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_NAME_BUNDLE));
                String provider = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_NAME_PROVIDER));
                PriceCheckRegion.Region region = PriceCheckRegion.fromString(cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_NAME_REGION)));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_NAME_NAME));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_NAME_CATEGORY));
                String image = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_NAME_IMAGE));
                String sku = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_NAME_SKU));
                String sellPrice = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_NAME_SELLPRICE));
                String buyPrice = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_NAME_CASHPRICE));
                String buyVoucherPrice = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_NAME_VOUCHERPRICE));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_NAME_DATE));
                ret.add(new PriceCheckProvider.PriceCheckItem(name, category, image, sku, sellPrice, buyPrice, buyVoucherPrice, provider, region, new Date(timestamp * 1000L)));
            } catch(IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        cursor.close();

        return ret;
    }

    public static boolean exists(SQLiteDatabase db, PriceCheckProvider.PriceCheckItem item, int bundle) {
        String[] projection = {
                PriceEntry._ID
        };

        Cursor cursor;
        if(bundle > 0) {
            String selection = PriceEntry.COLUMN_NAME_BUNDLE + " = ?" +
                    " AND " + PriceEntry.COLUMN_NAME_PROVIDER + " = ?" +
                    " AND " + PriceEntry.COLUMN_NAME_REGION + " = ?" +
                    " AND " + PriceEntry.COLUMN_NAME_SKU + " = ?" +
                    " AND " + PriceEntry.COLUMN_NAME_NAME + " = ?";

            String[] selectionArgs = {
                    Integer.toString(bundle),
                    item.getProvider(),
                    PriceCheckRegion.toString(item.getRegion()),
                    item.getSKU(),
                    item.getName()
            };

            cursor = db.query(
                    PriceEntry.TABLE_NAME,                     // The table to query
                    projection,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                 // The sort order
            );
        } else {
            String selection = PriceEntry.COLUMN_NAME_PROVIDER + " = ?" +
                    " AND " + PriceEntry.COLUMN_NAME_REGION + " = ?" +
                    " AND " + PriceEntry.COLUMN_NAME_SKU + " = ?" +
                    " AND " + PriceEntry.COLUMN_NAME_NAME + " = ?";

            String[] selectionArgs = {
                    item.getProvider(),
                    PriceCheckRegion.toString(item.getRegion()),
                    item.getSKU(),
                    item.getName()
            };

            cursor = db.query(
                    PriceEntry.TABLE_NAME,                     // The table to query
                    projection,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                 // The sort order
            );
        }

        boolean ret = cursor != null && cursor.getCount() > 0;
        if(cursor != null) {
            cursor.close();
        }
        return ret;
    }

    private static boolean insert(SQLiteDatabase db, PriceCheckProvider.PriceCheckItem item, int bundle) {
        ContentValues values = PriceEntry.getContentValues(item, bundle);
        return db.insert(PriceEntry.TABLE_NAME, null, values) >= 0;
    }

    private static boolean remove(SQLiteDatabase db, PriceCheckProvider.PriceCheckItem item, int bundle) {
        if(bundle > 0) {
            String selection = PriceEntry.COLUMN_NAME_BUNDLE + " = ?" +
                    " AND " + PriceEntry.COLUMN_NAME_PROVIDER + " = ?" +
                    " AND " + PriceEntry.COLUMN_NAME_REGION + " = ?" +
                    " AND " + PriceEntry.COLUMN_NAME_SKU + " = ?" +
                    " AND " + PriceEntry.COLUMN_NAME_NAME + " = ?";

            String[] selectionArgs = {
                    Integer.toString(bundle),
                    item.getProvider(),
                    PriceCheckRegion.toString(item.getRegion()),
                    item.getSKU(),
                    item.getName()
            };

            return db.delete(PriceEntry.TABLE_NAME, selection, selectionArgs) > 0;
        }

        String selection = PriceEntry.COLUMN_NAME_PROVIDER + " = ?" +
                " AND " + PriceEntry.COLUMN_NAME_REGION + " = ?" +
                " AND " + PriceEntry.COLUMN_NAME_SKU + " = ?" +
                " AND " + PriceEntry.COLUMN_NAME_NAME + " = ?";

        String[] selectionArgs = {
                item.getProvider(),
                PriceCheckRegion.toString(item.getRegion()),
                item.getSKU(),
                item.getName()
        };

        return db.delete(PriceEntry.TABLE_NAME, selection, selectionArgs) > 0;
    }

    public static class PriceCheckClearTask extends AsyncTask<Void, Void, Boolean> {
        private PriceCheckDatabase mDB;
        private int mBundle;
        private ClearListener mListener;

        public PriceCheckClearTask(PriceCheckDatabase db, int bundle) {
            mDB = db;
            mBundle = bundle;
        }

        public PriceCheckClearTask setResultListener(ClearListener listener) {
            mListener = listener;
            return this;
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            SQLiteDatabase db = mDB.getWritableDatabase();
            return PriceCheckDatabase.clear(db, mBundle);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(mListener != null) {
                mListener.onResult(result);
            }
        }
    }

    public static class PriceCheckGetAllTask extends AsyncTask<Void, Void, PriceCheckProvider.PriceCheckItems> {
        private PriceCheckDatabase mDB;
        private int mBundle;
        private GetAllListener mListener;

        public PriceCheckGetAllTask(PriceCheckDatabase db, int bundle) {
            mDB = db;
            mBundle = bundle;
        }

        public PriceCheckGetAllTask setResultListener(GetAllListener listener) {
            mListener = listener;
            return this;
        }

        @Override
        protected PriceCheckProvider.PriceCheckItems doInBackground(Void... args) {
            SQLiteDatabase db = mDB.getWritableDatabase();

            PriceCheckProvider.PriceCheckItems items = new PriceCheckProvider.PriceCheckItems();
            items.addAll(PriceCheckDatabase.getAll(db, mBundle));
            return items;
        }

        @Override
        protected void onPostExecute(PriceCheckProvider.PriceCheckItems result) {
            if(mListener != null) {
                mListener.onResult(result);
            }
        }
    }

    public static class PriceCheckUpdateTask extends AsyncTask<Void, Void, Boolean> {
        private PriceCheckDatabase mDB;
        private List<PriceCheckProvider.PriceCheckItem> mItems = new ArrayList<>();
        private int mBundle;
        private boolean mRemove = false;
        private UpdateListener mListener;

        public PriceCheckUpdateTask(PriceCheckDatabase db, PriceCheckProvider.PriceCheckItem item, int bundle) {
            mDB = db;
            mItems.add(item);
            mBundle = bundle;
            mRemove = !item.isSaved();
        }

        public PriceCheckUpdateTask(PriceCheckDatabase db, List<PriceCheckProvider.PriceCheckItem> items, int bundle) {
            mDB = db;
            mItems.addAll(items);
            mBundle = bundle;
            if(mItems.size() > 0) {
                mRemove = !mItems.get(0).isSaved();
            }
        }

        public PriceCheckUpdateTask setResultListener(UpdateListener listener) {
            mListener = listener;
            return this;
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            SQLiteDatabase db = mDB.getWritableDatabase();
            if(mRemove) {
                for(PriceCheckProvider.PriceCheckItem item: mItems) {
                    if(!PriceCheckDatabase.remove(db, item, mBundle)) {
                        return false;
                    }
                }

                return true;
            }

            for(PriceCheckProvider.PriceCheckItem item: mItems) {
                if (!PriceCheckDatabase.insert(db, item, mBundle)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(mListener != null) {
                mListener.onResult(result);
            }
        }
    }

    public interface ClearListener {
        void onResult(boolean result);
    }

    public interface GetAllListener {
        void onResult(PriceCheckProvider.PriceCheckItems results);
    }

    public interface UpdateListener {
        void onResult(boolean result);
    }

    /* Inner class that defines the table contents */
    private static class PriceEntry implements BaseColumns {
        static final String TABLE_NAME = "saved_list";
        static final String COLUMN_NAME_BUNDLE = "bundle";
        static final String COLUMN_NAME_PROVIDER = "provider";
        static final String COLUMN_NAME_REGION = "region";
        static final String COLUMN_NAME_NAME = "name";
        static final String COLUMN_NAME_IMAGE = "image";
        static final String COLUMN_NAME_SKU = "sku";
        static final String COLUMN_NAME_CATEGORY = "category";
        static final String COLUMN_NAME_SELLPRICE = "sellprice";
        static final String COLUMN_NAME_CASHPRICE = "cashprice";
        static final String COLUMN_NAME_VOUCHERPRICE = "voucherprice";
        static final String COLUMN_NAME_DATE = "date";

        static ContentValues getContentValues(PriceCheckProvider.PriceCheckItem item, int bundle) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_BUNDLE, bundle);
            values.put(COLUMN_NAME_PROVIDER, item.getProvider());
            values.put(COLUMN_NAME_REGION, PriceCheckRegion.toString(item.getRegion()));
            values.put(COLUMN_NAME_NAME, item.getName());
            values.put(COLUMN_NAME_IMAGE, item.getImage());
            values.put(COLUMN_NAME_SKU, item.getSKU());
            values.put(COLUMN_NAME_CATEGORY, item.getCategory());
            values.put(COLUMN_NAME_SELLPRICE, item.getSellPrice());
            values.put(COLUMN_NAME_CASHPRICE, item.getBuyPrice());
            values.put(COLUMN_NAME_VOUCHERPRICE, item.getBuyVoucherPrice());
            values.put(COLUMN_NAME_DATE, System.currentTimeMillis() / 1000L);

            return values;
        }
    }

}