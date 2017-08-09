package com.matnar.app.android.flippi.pricecheck;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class PriceCheckCategories extends ArrayList<PriceCheckCategories.PriceCheckCategory> implements Parcelable {
    public PriceCheckCategories() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(size());
        for(PriceCheckCategory item: this) {
            item.writeToParcel(dest, flags);
        }
    }

    public void addAll(Parcelable other) {
        addAll((PriceCheckCategories) other);
    }

    public void addAll(PriceCheckCategories other) {
        for(Parcelable p: other) {
            add((PriceCheckCategory) p);
        }
    }


    PriceCheckCategories(Parcel in) {
        int size = in.readInt();
        for(int i = 0; i < size; i++) {
            add(new PriceCheckCategory(in));
        }
    }

    public static final Parcelable.Creator<PriceCheckProvider.PriceCheckItems> CREATOR = new Parcelable.Creator<PriceCheckProvider.PriceCheckItems>() {
        public PriceCheckProvider.PriceCheckItems createFromParcel(Parcel source) {
            return new PriceCheckProvider.PriceCheckItems(source);
        }

        public PriceCheckProvider.PriceCheckItems[] newArray(int size) {
            return new PriceCheckProvider.PriceCheckItems[size];
        }
    };

    public static class PriceCheckCategory implements Parcelable {
        private long mID;
        private String mName;
        private PriceCheckCategories mChildren = new PriceCheckCategories();

        public PriceCheckCategory(long id, String name) {
            mID = id;
            mName = name;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(mID);
            dest.writeString(mName);
            dest.writeInt(mChildren.size());
            for(PriceCheckCategory item: mChildren) {
                item.writeToParcel(dest, flags);
            }
        }

        public void addChildren(PriceCheckCategories items) {
            mChildren.addAll(items);
        }

        public long getID() {
            return mID;
        }

        public String getName() {
            return mName;
        }

        public PriceCheckCategories getChildren() {
            return mChildren;
        }

        PriceCheckCategory(Parcel in) {
            mID = in.readLong();
            mName = in.readString();
            int size = in.readInt();
            for(int i = 0; i < size; i++) {
                mChildren.add(new PriceCheckCategory(in));
            }
        }

        public static final Parcelable.Creator<PriceCheckCategory> CREATOR = new Parcelable.Creator<PriceCheckCategory>() {
            public PriceCheckCategory createFromParcel(Parcel source) {
                return new PriceCheckCategory(source);
            }

            public PriceCheckCategory[] newArray(int size) {
                return new PriceCheckCategory[size];
            }
        };
    }
}
