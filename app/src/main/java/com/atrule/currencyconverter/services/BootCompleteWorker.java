package com.atrule.currencyconverter.services;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.atrule.currencyconverter.receivers.MyReceiver;
import com.atrule.currencyconverter.receivers.RateReceiver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BootCompleteWorker extends Worker {
    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private Intent intentNew;
    public Context myContext;

    public BootCompleteWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        myContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        setCurrencyAlarm(myContext);
        setRateAlarm(myContext);

        return Result.success();
    }

    //region set alarm for currency converter
    public void setCurrencyAlarm(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("Notify Me", MODE_PRIVATE);
        if(sharedPreferences.contains("time")){
            if(!sharedPreferences.getString("time", "").equals("Notify Me?")){
                String time = sharedPreferences.getString("time", "");

                Calendar calendar = Calendar.getInstance();
                try {
                    calendar.setTime(simpleDateFormat.parse(time));

                    intentNew = new Intent(context, MyReceiver.class);
                    intentNew.setAction("MyAlarm");
                    alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                        alarmIntent = PendingIntent.getBroadcast(context, 201, intentNew, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                    }
                    else{
                        alarmIntent = PendingIntent.getBroadcast(context, 201, intentNew, PendingIntent.FLAG_UPDATE_CURRENT);
                    }

                    if (alarmIntent != null && alarmMgr != null) {
                        alarmMgr.cancel(alarmIntent);
                    }

                    if (alarmMgr != null) {
                        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                                24*60*60*1000, alarmIntent);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //endregion

    //region set alarm for rates
    public void setRateAlarm(Context context){
        SharedPreferences prefs = context.getSharedPreferences("notification_setting", MODE_PRIVATE);
        if(prefs.contains("alarm")){
            if(prefs.getBoolean("alarm", false)){
                intentNew = new Intent(context, RateReceiver.class);
                intentNew.setAction("RateReceiver");
                alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                    alarmIntent = PendingIntent.getBroadcast(context, 201, intentNew, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                }
                else{
                    alarmIntent = PendingIntent.getBroadcast(context, 201, intentNew, PendingIntent.FLAG_UPDATE_CURRENT);
                }

                if (alarmIntent != null && alarmMgr != null) {
                    alarmMgr.cancel(alarmIntent);
                }

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, 10);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        12 * 60 * 60 * 1000, alarmIntent);
            }
        }
    }
    //endregion
}
