package com.atrule.currencyconverter.util;

import static android.content.Context.CONNECTIVITY_SERVICE;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.fragment.app.FragmentActivity;

public class MyUtil {
    //region Check if we have network connection
    public static boolean haveNetwork(FragmentActivity activity){
        boolean haveWifi = false;
        boolean haveMobileData = false;

        if(activity != null){
            ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo[] allNetworkInfo = connectivityManager.getAllNetworkInfo();
            for (NetworkInfo networkInfo : allNetworkInfo){
                if (networkInfo.getTypeName().equalsIgnoreCase("WIFI")){
                    if (networkInfo.isConnected()){
                        haveWifi = true;
                    }
                }

                if (networkInfo.getTypeName().equalsIgnoreCase("MOBILE")){
                    if (networkInfo.isConnected()){
                        haveMobileData = true;
                    }
                }
            }
        }
        return haveMobileData || haveWifi;
    }
    //endregion
}