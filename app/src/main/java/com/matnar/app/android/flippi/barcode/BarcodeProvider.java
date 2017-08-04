package com.matnar.app.android.flippi.barcode;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.matnar.app.android.flippi.barcode.provider.BuycottBarcodeProvider;
import com.matnar.app.android.flippi.barcode.provider.EandatabaseBarcodeProvider;
import com.matnar.app.android.flippi.barcode.provider.OutpanBarcodeProvider;

import java.lang.ref.WeakReference;

public abstract class BarcodeProvider {
    private String[] KEYWORDS = {
            /* Nintendo */
            "NES",
            "SNES",
            "N64",

            /* SEGA */
            "SEGA",

            /* Sony */
            "PSX",
            "PS1",
            "PS2",

            /* Publishers */
            "EIDOS",
            "PSYGNOSIS"
    };

    public static void getInformation(WeakReference<Context> context, String barcode, BarcodeListener listener) {
        new BarcodeTask(context, listener).execute(barcode);
    }

    protected abstract BarcodeInformation lookup(String barcode);

    final protected String format(String name) {
        StringBuilder sb = new StringBuilder();
        for(String k: KEYWORDS) {
            if(sb.length() > 0) {
                sb.append("|");
            }

            sb.append(k);
        }

        name = name.replaceAll("\\b(?i:" + sb.toString() + ")\\b", "");
        return name.trim();
    }

    public interface BarcodeListener {
        void onResult(BarcodeInformation result);
    }

    public static final class BarcodeInformation {
        private String mName;

        public BarcodeInformation(String name) {
            mName = name;
        }

        public String getName() {
            return mName;
        }
    }

    private static class BarcodeTask extends AsyncTask<String, Void, BarcodeInformation> {
        private WeakReference<Context> mContext;
        private BarcodeListener mListener;

        BarcodeTask(WeakReference<Context> context, BarcodeListener listener) {
            mContext = context;
            mListener = listener;
        }


        @Override
        protected BarcodeProvider.BarcodeInformation doInBackground(String... strings) {
            if(mContext.get() == null) {
                return null;
            }

            String providerName = PreferenceManager.getDefaultSharedPreferences(mContext.get()).getString("barcode_provider", "outpan");

            BarcodeProvider provider = null;
            switch (providerName) {
                case "outpan":
                    provider = new OutpanBarcodeProvider(mContext);
                    break;
                case "buycott":
                    provider = new BuycottBarcodeProvider(mContext);
                    break;
                case "eandatabase":
                    provider = new EandatabaseBarcodeProvider(mContext);
                    break;
            }

            return (provider != null) ? provider.lookup(strings[0]) : null;
        }

        @Override
        protected void onPostExecute(BarcodeProvider.BarcodeInformation info) {
            if(mListener != null) {
                mListener.onResult(info);
            }
        }
    }
}
