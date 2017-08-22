package com.matnar.app.android.flippi.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.provider.BaseColumns;

import com.matnar.app.android.flippi.pricecheck.PriceCheckCategories;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class CategoryDatabase extends SQLiteAssetHelper {
    private static final String TAG = "Flippi." + CategoryDatabase.class.getSimpleName();

    private static final String DATABASE_NAME = "categories.db";
    private static final int DATABASE_VERSION = 3;

    public CategoryDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        setForcedUpgrade(Integer.MAX_VALUE);
    }

    private static PriceCheckCategories getAll(SQLiteDatabase db, String provider) {
        return getAll(db, provider, 0);
    }

    private static PriceCheckCategories getAll(SQLiteDatabase db, String provider, long parent) {
        PriceCheckCategories ret = new PriceCheckCategories();

        String[] projection = {
                CategoryEntry._ID,
                CategoryEntry.COLUMN_NAME_NAME,
                CategoryEntry.COLUMN_NAME_PARENT,
                CategoryEntry.COLUMN_NAME_PROVIDER,
                CategoryEntry.COLUMN_NAME_CATEGORYID
        };

        String sortOrder =
                CategoryEntry.COLUMN_NAME_NAME + " ASC";

        String selection = CategoryEntry.COLUMN_NAME_PROVIDER + " = ?"
                + " AND " + CategoryEntry.COLUMN_NAME_PARENT + " = ?";

        String[] selectionArgs = {
                provider,
                Long.toString(parent)
        };

        Cursor cursor = db.query(
                CategoryEntry.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        while(cursor.moveToNext()) {
            try {
                long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(CategoryEntry._ID));
                long itemParent = cursor.getLong(cursor.getColumnIndexOrThrow(CategoryEntry.COLUMN_NAME_PARENT));
                String itemName = cursor.getString(cursor.getColumnIndexOrThrow(CategoryEntry.COLUMN_NAME_NAME));
                String itemProvider = cursor.getString(cursor.getColumnIndexOrThrow(CategoryEntry.COLUMN_NAME_PROVIDER));
                long categoryID = cursor.getLong(cursor.getColumnIndexOrThrow(CategoryEntry.COLUMN_NAME_CATEGORYID));

                PriceCheckCategories.PriceCheckCategory item = new PriceCheckCategories.PriceCheckCategory(itemId, itemName, categoryID);
                item.addChildren(getAll(db, provider, itemId));

                ret.add(item);
            } catch(IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        cursor.close();

        return ret;
    }

    public static class GetAllTask extends AsyncTask<Void, Void, PriceCheckCategories> {
        private CategoryDatabase mDB;
        private String mProvider;
        private GetAllListener mListener;

        public GetAllTask(CategoryDatabase db, String provider) {
            mDB = db;
            mProvider = provider;
        }

        public GetAllTask setResultListener(GetAllListener listener) {
            mListener = listener;
            return this;
        }

        @Override
        protected PriceCheckCategories doInBackground(Void... args) {
            SQLiteDatabase db = mDB.getReadableDatabase();
            return CategoryDatabase.getAll(db, mProvider);
        }

        @Override
        protected void onPostExecute(PriceCheckCategories result) {
            if(mListener != null) {
                mListener.onResult(result);
            }
        }
    }

    public interface GetAllListener {
        void onResult(PriceCheckCategories results);
    }

    /* Inner class that defines the table contents */
    private static class CategoryEntry implements BaseColumns {
        static final String TABLE_NAME = "categories";
        static final String COLUMN_NAME_NAME = "name";
        static final String COLUMN_NAME_PARENT = "parent";
        static final String COLUMN_NAME_PROVIDER = "provider";
        static final String COLUMN_NAME_CATEGORYID = "categoryID";
    }
}
