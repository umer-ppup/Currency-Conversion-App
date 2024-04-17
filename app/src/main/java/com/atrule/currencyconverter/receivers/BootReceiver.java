package com.atrule.currencyconverter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.atrule.currencyconverter.services.BootCompleteWorker;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(BootCompleteWorker.class).build();
            WorkManager.getInstance(context).enqueue(oneTimeWorkRequest);
        }
    }
}
