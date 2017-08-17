package com.matnar.app.android.flippi.view.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.matnar.app.android.flippi.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class SavedSearchesAdapter extends ArrayAdapter<String> {
    private static final String TAG = "Flippi." + SavedSearchesAdapter.class.getSimpleName();

    static final String SEARCHES_NAME = "searches.bin";

    private List<String> mItems = new ArrayList<>();
    boolean mItemsChanged = false;

    public SavedSearchesAdapter(@NonNull Context context) {
        super(context, R.layout.saved_searches_row);

        update();
    }

    @Override
    public void add(@Nullable String str) {
        if(mItems.indexOf(str) == 0) {
            return;
        }

        mItems.add(0, str);
        super.insert(str, 0);

        mItemsChanged = true;
    }

    public void update() {
        FileInputStream stream = null;
        try {
            stream = getContext().openFileInput(SEARCHES_NAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            String line;
            while((line = reader.readLine()) != null) {
                if(line.length() == 0 || mItems.contains(line)) {
                    continue;
                }

                mItems.add(line);
            }
        } catch (java.io.IOException e) {
            // Empty
        }

        if(stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        super.clear();
        super.addAll(mItems);
    }

    public void save() {
        // Do we actually need to save?
        if(!mItemsChanged) {
            return;
        }

        FileOutputStream stream = null;
        try {
            stream = getContext().openFileOutput(SEARCHES_NAME, Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(stream);

            for(String item: mItems) {
                writer.write(item + "\n");
            }
            writer.flush();
            mItemsChanged = false;
        } catch(IOException e) {
            Log.e(TAG, "Could not save searches", e);
        }

        if(stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
