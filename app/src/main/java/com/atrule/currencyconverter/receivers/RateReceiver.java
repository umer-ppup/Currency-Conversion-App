package com.atrule.currencyconverter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.atrule.currencyconverter.services.RateWorker;

public class RateReceiver extends BroadcastReceiver {

    public RateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("RateReceiver")) {

            OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(RateWorker.class).build();
            WorkManager.getInstance(context).enqueue(oneTimeWorkRequest);
        }
    }
}