package com.atrule.currencyconverter.activities;

import static com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.atrule.currencyconverter.R;
import com.atrule.currencyconverter.api.RetrofitClient;
import com.atrule.currencyconverter.classes.CurrencyRates;
import com.atrule.currencyconverter.fragments.ConverterFragment;
import com.atrule.currencyconverter.fragments.GoldRateFragment;
import com.atrule.currencyconverter.fragments.RateFragment;
import com.atrule.currencyconverter.receivers.MyReceiver;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.stream.Collectors;

import eu.dkaratzas.android.inapp.update.Constants;
import eu.dkaratzas.android.inapp.update.InAppUpdateManager;
import eu.dkaratzas.android.inapp.update.InAppUpdateStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavigationActivity extends AppCompatActivity implements InAppUpdateManager.InAppUpdateHandler {

    //region declarations
    private BottomNavigationView bottom_navigation;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private Intent intent;
    private SharedPreferences prefForRates;
    private SharedPreferences sharedPreferences1;
    private HashMap<String, Double> jsonRates;
    private Toolbar toolbar;

    private RateFragment rateFragment;
    private ConverterFragment converterFragment;
    private GoldRateFragment goldRateFragment;
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment active;
    private final String notify = "Notify Me?";
    private MenuItem menuItem, bellIcon;
    private final NavigationBarView.OnItemSelectedListener mOnNavigationItemSelectedListener
            = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            int id = item.getItemId();
            if(id == R.id.item_gold_rate){
                if(goldRateFragment != null){
                    if(active != null){
                        toolbar.setTitle("Gold Rates");
                        fm.beginTransaction().hide(active).show(goldRateFragment).commit();
                        active = goldRateFragment;
                        return true;
                    }
                }
            }
            else if(id == R.id.item_rate){
                if(rateFragment != null){
                    if(active != null){
                        toolbar.setTitle("Rates");
                        fm.beginTransaction().hide(active).show(rateFragment).commit();
                        active = rateFragment;
                        return true;
                    }
                }
            }
            else if(id == R.id.item_converter){
                if(converterFragment != null){
                    if(active != null){
                        toolbar.setTitle("Converter");
                        fm.beginTransaction().hide(active).show(converterFragment).commit();
                        active = converterFragment;
                        return true;
                    }
                }
            }
            return false;
        }
    };

    // Declare the InAppUpdateManager & ReviewManager
    private InAppUpdateManager inAppUpdateManager;
    private ReviewManager reviewManager;
    final int REQ_CODE_VERSION_UPDATE = 1000;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //region view setting
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Rates");
        }
        //endregion

        // region InAppUpdate/UpdateManager
        inAppUpdateManager = InAppUpdateManager.Builder(NavigationActivity.this, REQ_CODE_VERSION_UPDATE)
                .resumeUpdates(true) // Resume the update, if the update was stalled. Default is true
                .mode(Constants.UpdateMode.FLEXIBLE)
                .snackBarMessage("An update has just been downloaded.")
                .snackBarAction("RESTART")
                .useCustomNotification(true)
                .handler(this);

        inAppUpdateManager.checkForAppUpdate();
        // endregion

        // region InAppReview
        reviewManager = ReviewManagerFactory.create(this);
        showRateApp();
        // endregion

        //region save rate for one time
        prefForRates = PreferenceManager.getDefaultSharedPreferences(NavigationActivity.this);
        if (prefForRates.getBoolean("NotSaved", true)) {
            String rateURL = "";

            Call<String> stringCall = RetrofitClient.getInstance().getMyApi().getResponse(rateURL);
            stringCall.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                    if(response.isSuccessful()){
                        String responseString = response.body();
                        if(responseString != null){
                            try {
                                String json = responseString.substring(responseString.indexOf("(") + 1, responseString.indexOf(")"));
                                Gson gson = new Gson();
                                CurrencyRates currencyRates = gson.fromJson(json, CurrencyRates.class);
                                String str = currencyRates.getValiutos();
                                jsonRates = new HashMap<>();
                                jsonRates = (HashMap<String, Double>) Arrays.stream(str.split(";")).map(s -> s.split(":")).collect(Collectors.toMap(e -> e[0], e -> Double.parseDouble(e[1])));

                                SharedPreferences s = getSharedPreferences("Rates", Context.MODE_PRIVATE);
                                SharedPreferences.Editor e = s.edit();
                                e.putString("jsonRate", jsonRates.toString());
                                if (e.commit()) {
                                    prefForRates.edit().putBoolean("NotSaved", false).apply();
                                } else {
                                    prefForRates.edit().putBoolean("NotSaved", true).apply();
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) { }
            });
        }
        //endregion

        //region initialization
        sharedPreferences1 = getSharedPreferences("PinCities", Context.MODE_PRIVATE);
        bottom_navigation = findViewById(R.id.bottom_navigation);
        bottom_navigation.setOnItemSelectedListener(mOnNavigationItemSelectedListener);
        bottom_navigation.setItemIconTintList(null);

        rateFragment = new RateFragment();
        active = rateFragment;
        converterFragment = new ConverterFragment();
        goldRateFragment = new GoldRateFragment();

        fm.beginTransaction().add(R.id.fragment_container, rateFragment, "1").commit();
        fm.beginTransaction().add(R.id.fragment_container, converterFragment, "2").hide(converterFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, goldRateFragment, "3").hide(goldRateFragment).commit();
        //endregion
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //region setting option menu according to alarm and related fragment
        getMenuInflater().inflate(R.menu.my_menu, menu);
        menuItem = menu.findItem(R.id.notify);
        bellIcon = menu.findItem(R.id.btnNotify);
        SharedPreferences sharedPreferences = getSharedPreferences("Notify Me", MODE_PRIVATE);
        String time = sharedPreferences.getString("time", "");
        if(time.equals("")){
            menuItem.setTitle(notify);
        }
        else{
            menuItem.setTitle(time);
        }

        menuItem.setVisible(false);
        bellIcon.setVisible(false);
        //endregion

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //region click handler to set and remove alarm
        if(item.getItemId() == R.id.btnNotify){
            if(!sharedPreferences1.getAll().isEmpty()){
                Calendar time = Calendar.getInstance();
                int hour = time.get(Calendar.HOUR_OF_DAY);
                int minute = time.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(NavigationActivity.this, (timePicker, selectedHour, selectedMinute) -> {
                    intent = new Intent(NavigationActivity.this, MyReceiver.class);
                    intent.setAction("MyAlarm");
                    alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                        alarmIntent = PendingIntent.getBroadcast(NavigationActivity.this, 201, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                    }
                    else{
                        alarmIntent = PendingIntent.getBroadcast(NavigationActivity.this, 201, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    }

                    if (alarmIntent != null && alarmMgr != null) {
                        alarmMgr.cancel(alarmIntent);
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                    calendar.set(Calendar.MINUTE, selectedMinute);

                    alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                            24*60*60*1000, alarmIntent);

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
                    String timeNew = simpleDateFormat.format(calendar.getTime());

                    SharedPreferences sharedPreferences = getSharedPreferences("Notify Me", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("time", timeNew);
                    editor.apply();

                    menuItem.setTitle(timeNew);

                    AlertDialog.Builder builder = new AlertDialog.Builder(NavigationActivity.this, R.style.MyAlertDialogStyle);
                    //builder.setTitle("You will be notified at "+timeNew+" everyday. And this will apply only for the countries that are pin.");
                    builder.setMessage("You will be notified at "+timeNew+" everyday. And this will apply only for the countries that are pin.");
                    builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                    builder.setCancelable(false);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setOnShowListener(dialog -> {
                        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                    });

                    alertDialog.show();
                }, hour, minute, false);//Yes 24 hour time
                mTimePicker.setTitle(getString(R.string.select_time_text));
                mTimePicker.show();
            }
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(NavigationActivity.this, R.style.MyAlertDialogStyle);
                builder.setMessage(R.string.not_pin_text);
                builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                builder.setCancelable(false);
                AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(dialog -> {
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                });

                alertDialog.show();
            }
        }
        else if(item.getItemId() == R.id.notify){
            if(!sharedPreferences1.getAll().isEmpty()){
                if(menuItem.getTitle().equals("Notify Me?")){
                    Calendar time = Calendar.getInstance();
                    int hour = time.get(Calendar.HOUR_OF_DAY);
                    int minute = time.get(Calendar.MINUTE);
                    TimePickerDialog mTimePicker;
                    mTimePicker = new TimePickerDialog(NavigationActivity.this, (timePicker, selectedHour, selectedMinute) -> {
                        intent = new Intent(NavigationActivity.this, MyReceiver.class);
                        intent.setAction("MyAlarm");
                        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                            alarmIntent = PendingIntent.getBroadcast(NavigationActivity.this, 201, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                        }
                        else{
                            alarmIntent = PendingIntent.getBroadcast(NavigationActivity.this, 201, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        }

                        if (alarmIntent != null && alarmMgr != null) {
                            alarmMgr.cancel(alarmIntent);
                        }

                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(System.currentTimeMillis());
                        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                        calendar.set(Calendar.MINUTE, selectedMinute);

                        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                                24*60*60*1000, alarmIntent);

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
                        String timeNew = simpleDateFormat.format(calendar.getTime());

                        SharedPreferences sharedPreferences = getSharedPreferences("Notify Me", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("time", timeNew);
                        editor.apply();

                        menuItem.setTitle(timeNew);

                        AlertDialog.Builder builder = new AlertDialog.Builder(NavigationActivity.this, R.style.MyAlertDialogStyle);
                        //builder.setTitle("You will be notified at "+timeNew+" everyday. And this will apply only for the countries that are pin.");
                        builder.setMessage("You will be notified at "+timeNew+" everyday. And this will apply only for the countries that are pin.");
                        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                        builder.setCancelable(false);
                        AlertDialog alertDialog = builder.create();
                        alertDialog.setOnShowListener(dialog -> {
                            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                        });

                        alertDialog.show();
                    }, hour, minute, false);//Yes 24 hour time
                    mTimePicker.setTitle(getString(R.string.select_time_text));
                    mTimePicker.show();
                }
                else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(NavigationActivity.this, R.style.MyAlertDialogStyle);
                    builder.setMessage("(1) Click CHANGE to change the time.\n(2) Click REMOVE to unsubscribe.");
                    builder.setPositiveButton("CHANGE", (dialog, which) -> {
                        Calendar time = Calendar.getInstance();
                        int hour = time.get(Calendar.HOUR_OF_DAY);
                        int minute = time.get(Calendar.MINUTE);
                        TimePickerDialog mTimePicker;
                        mTimePicker = new TimePickerDialog(NavigationActivity.this, (timePicker, selectedHour, selectedMinute) -> {
                            intent = new Intent(NavigationActivity.this, MyReceiver.class);
                            intent.setAction("MyAlarm");
                            alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                                alarmIntent = PendingIntent.getBroadcast(NavigationActivity.this, 201, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                            }
                            else{
                                alarmIntent = PendingIntent.getBroadcast(NavigationActivity.this, 201, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            }

                            if (alarmIntent != null && alarmMgr != null) {
                                alarmMgr.cancel(alarmIntent);
                            }

                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeInMillis(System.currentTimeMillis());
                            calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                            calendar.set(Calendar.MINUTE, selectedMinute);

                            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                                    24 * 60 * 60 * 1000, alarmIntent);

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
                            String timeNew = simpleDateFormat.format(calendar.getTime());

                            SharedPreferences sharedPreferences = getSharedPreferences("Notify Me", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("time", timeNew);
                            editor.apply();

                            menuItem.setTitle(timeNew);

                            AlertDialog.Builder builder1 = new AlertDialog.Builder(NavigationActivity.this, R.style.MyAlertDialogStyle);
                            //builder.setTitle("You will be notified at "+timeNew+" everyday. And this will apply only for the countries that are pin.");
                            builder1.setMessage("You will be notified at " + timeNew + " everyday. And this will apply only for the countries that are pin.");
                            builder1.setPositiveButton("OK", (dialog12, which1) -> dialog12.dismiss());
                            builder1.setCancelable(false);
                            AlertDialog alertDialog = builder1.create();
                            alertDialog.setOnShowListener(dialog1 -> {
                                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                            });

                            alertDialog.show();
                        }, hour, minute, false);//Yes 24 hour time
                        mTimePicker.setTitle("Change time on which you will be notified everyday.");
                        mTimePicker.show();
                        dialog.dismiss();
                    });
                    builder.setNegativeButton("REMOVE", (dialog, which) -> {
                        intent = new Intent(NavigationActivity.this, MyReceiver.class);
                        intent.setAction("MyAlarm");
                        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                            alarmIntent = PendingIntent.getBroadcast(NavigationActivity.this, 201, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_MUTABLE);
                        }
                        else{
                            alarmIntent = PendingIntent.getBroadcast(NavigationActivity.this, 201, intent, PendingIntent.FLAG_NO_CREATE);
                        }

                        if (alarmIntent != null && alarmMgr != null) {
                            alarmMgr.cancel(alarmIntent);
                        }

                        SharedPreferences sharedPreferences = getSharedPreferences("Notify Me", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("time", "Notify Me?");
                        editor.apply();

                        menuItem.setTitle("Notify Me?");

                        dialog.dismiss();
                    });
                    builder.setCancelable(true);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setOnShowListener(dialog -> {
                        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                    });

                    alertDialog.show();
                }
            }
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(NavigationActivity.this, R.style.MyAlertDialogStyle);
                builder.setMessage(R.string.not_pin_text);
                builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                builder.setCancelable(false);
                AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(dialog -> {
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                });

                alertDialog.show();
            }
        }
        //endregion
        return super.onOptionsItemSelected(item);
    }

    // region InAppUpdateHandler
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Data", "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_VERSION_UPDATE) {
            //region variables
            String TAG = "MainActivity";
            switch (resultCode) {
                case Activity.RESULT_OK:
                    Log.d(TAG, "App download started...");
//                    Toast.makeText(MainActivity.this, "App download started...", Toast.LENGTH_LONG).show();
                    break;
                case Activity.RESULT_CANCELED:
                    // If the update is cancelled by the user,
                    // you can request to start the update again.
//                    inAppUpdateManager.checkForAppUpdate();
                    Log.d(TAG, "App download canceled." + resultCode);
                    break;
                case RESULT_IN_APP_UPDATE_FAILED:
                    Toast.makeText(NavigationActivity.this, "App download failed....", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    @Override
    public void onInAppUpdateError(int code, Throwable error) {

        Log.d("inAppUpdateManager", "onInAppUpdateError : " + error.getMessage());
    }

    @Override
    public void onInAppUpdateStatus(InAppUpdateStatus status) {

        Log.d("inAppUpdateManager", "onInAppUpdateStatus : " + status.isUpdateAvailable());
        Log.d("inAppUpdateManager", "onInAppUpdateStatus : " + status.availableVersionCode());
        Log.d("inAppUpdateManager", "onInAppUpdateStatus : " + status.isDownloaded());
        Log.d("inAppUpdateManager", "onInAppUpdateStatus : " + status.isDownloading());
        Log.d("inAppUpdateManager", "onInAppUpdateStatus : " + status.isFailed());

        /*
         * If the update downloaded, ask user confirmation and complete the update
         */
        if (status.isDownloaded()) {

            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);

            Snackbar snackbar = Snackbar.make(rootView,
                    "An update has just been downloaded.",
                    Snackbar.LENGTH_INDEFINITE);

            snackbar.setAction("RESTART", view -> {

                // Triggers the completion of the update of the app for the flexible flow.
                inAppUpdateManager.completeUpdate();
            });

//            snackbar.show();
        }
    }
    // endregion

    // Shows the app rate dialog box using In-App review API
    // The app rate dialog box might or might not shown depending
    // on the Quotas and limitations
    public void showRateApp() {
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        Log.d("showRateApp", "request : " + request.toString());
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Getting the ReviewInfo object
                ReviewInfo reviewInfo = task.getResult();

                Task <Void> flow = reviewManager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(task1 -> {
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown.
                });
            }
        });
    }
}