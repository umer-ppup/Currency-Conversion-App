package com.atrule.currencyconverter.classes;

import android.os.Parcel;
import android.os.Parcelable;

public class ScrapData implements Parcelable {
    //region member variables
    String currency, symbol, buying, selling;
    int flag;
    //endregion

    //region constructor
    public ScrapData() {
    }
    //endregion

    protected ScrapData(Parcel in) {
        currency = in.readString();
        symbol = in.readString();
        buying = in.readString();
        selling = in.readString();
        flag = in.readInt();
    }

    public static final Creator<ScrapData> CREATOR = new Creator<ScrapData>() {
        @Override
        public ScrapData createFromParcel(Parcel in) {
            return new ScrapData(in);
        }

        @Override
        public ScrapData[] newArray(int size) {
            return new ScrapData[size];
        }
    };

    //region getters and setters
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getBuying() {
        return buying;
    }

    public void setBuying(String buying) {
        this.buying = buying;
    }

    public String getSelling() {
        return selling;
    }

    public void setSelling(String selling) {
        this.selling = selling;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(currency);
        dest.writeString(symbol);
        dest.writeString(buying);
        dest.writeString(selling);
        dest.writeInt(flag);
    }
    //endregion

}
