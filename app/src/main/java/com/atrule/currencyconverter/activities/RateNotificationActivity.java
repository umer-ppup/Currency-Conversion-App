package com.atrule.currencyconverter.activities;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.atrule.currencyconverter.R;
import com.atrule.currencyconverter.classes.NotificationID;
import com.atrule.currencyconverter.classes.ScrapData;
import com.atrule.currencyconverter.receivers.RateReceiver;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class RateNotificationActivity extends AppCompatActivity {
    //region Variables declarations
    private Toolbar toolbar;
    private ArrayList<ScrapData> scrapDataArrayList;
    private ArrayList<String> currency;
    private List<String> buySell;
    private List<String> condition;
    private Spinner spCurrency, spBuySell, spCondition;
    private EditText etValue;
    private Switch switch1;
    private Button button;
    private final HashMap<String, String> settings = new HashMap<>();
    private HashMap<String, String> saveSettings = new HashMap<>();
    private ProgressDialog progressDialog;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private Intent intent;
    private boolean switchValue;
    private int buySellPosition, conditionPosition, currencyPosition;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_notification);

        //region Get back all save data if exists
        SharedPreferences prefs = getSharedPreferences("notification_setting", MODE_PRIVATE);
        switchValue = prefs.getBoolean("alarm", false);

        String storedHashMapString = prefs.getString("setting", "");
        java.lang.reflect.Type type = new TypeToken<HashMap<String, String>>() {
        }.getType();
        Gson gson = new Gson();
        if (!storedHashMapString.equals("")) {
            saveSettings = gson.fromJson(storedHashMapString, type);
        }
        //endregion

        //region toolbar related code
        toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Notification Setting");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //endregion

        //region initializing arraylists
        scrapDataArrayList = new ArrayList<>();
        currency = new ArrayList<>();
        scrapDataArrayList = getIntent().getParcelableArrayListExtra("rates");
        //endregion

        //region initializing arraylists for spinners
        for (int i = 0; i < scrapDataArrayList.size(); i++) {
            currency.add(scrapDataArrayList.get(i).getSymbol());
        }

        buySell = Arrays.asList(getResources().getStringArray(R.array.buy_sell));
        condition = Arrays.asList(getResources().getStringArray(R.array.condition));
        //endregion

        //region find view by ids
        spCurrency = findViewById(R.id.spCurrency);
        spBuySell = findViewById(R.id.spBuySell);
        spCondition = findViewById(R.id.spCondition);
        etValue = findViewById(R.id.etValue);
        switch1 = findViewById(R.id.switch1);
        button = findViewById(R.id.button);
        //endregion

        //region setting alarm switch value based on the previous value
        if (switchValue) {
            switch1.setChecked(true);
            switch1.setText("On");
        } else {
            switch1.setChecked(false);
            switch1.setText("Off");
        }
        //endregion

        //region progress dialog
        progressDialog = new ProgressDialog(RateNotificationActivity.this, R.style.MyAlertDialogStyle);
        progressDialog.setMessage("Saving...");
        progressDialog.setCanceledOnTouchOutside(false);
        //endregion

        //region spinner adapters code
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, currency);
        spCurrency.setAdapter(spinnerArrayAdapter);

        ArrayAdapter<String> spinnerArrayAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, buySell);
        spBuySell.setAdapter(spinnerArrayAdapter1);

        ArrayAdapter<String> spinnerArrayAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, condition);
        spCondition.setAdapter(spinnerArrayAdapter2);
        //endregion

        //region setting view based on previous values
        if (!saveSettings.isEmpty()) {
            positionSetter();
            spCurrency.setSelection(currencyPosition);
            spBuySell.setSelection(buySellPosition);
            spCondition.setSelection(conditionPosition);
            etValue.setText(saveSettings.get("value"));
        }
        //endregion

        //region alarm switch handler
        switch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //region set alarm
            if (isChecked) {
                progressDialog.show();
                intent = new Intent(RateNotificationActivity.this, RateReceiver.class);
                intent.setAction("RateReceiver");
                alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                    alarmIntent = PendingIntent.getBroadcast(RateNotificationActivity.this, 201, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                }
                else{
                    alarmIntent = PendingIntent.getBroadcast(RateNotificationActivity.this, 201, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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

                //save in shared prefs
                SharedPreferences prefs12 = getSharedPreferences("notification_setting", MODE_PRIVATE);
                if (prefs12.edit().putBoolean("alarm", true).commit()) {
                    progressDialog.hide();
                }

                switch1.setText("On");

                //region show notification if required condition satisfy if current time is before the actual alarm time of our app
                if (Calendar.getInstance().before(calendar)) {
                    if (!saveSettings.isEmpty()) {
                        for (int j = 0; j < scrapDataArrayList.size(); j++) {
                            String condition = getCondition();
                            if (scrapDataArrayList.get(j).getSymbol().equalsIgnoreCase(saveSettings.get("currency"))) {
                                if (saveSettings.get("type").equals("Buying")) {
                                    if (condition.equals(">")) {
                                        if (Double.parseDouble(scrapDataArrayList.get(j).getBuying()) > Double.parseDouble(saveSettings.get("value"))) {
                                            sendNotification("Buying Rates are " + saveSettings.get("condition") + " " + saveSettings.get("value") + "(" + scrapDataArrayList.get(j).getBuying() + ")" + " for " + saveSettings.get("currency"));
                                        }
                                    } else if (condition.equals(">=")) {
                                        if (Double.parseDouble(scrapDataArrayList.get(j).getBuying()) >= Double.parseDouble(saveSettings.get("value"))) {
                                            sendNotification("Buying Rates are " + saveSettings.get("condition") + " " + saveSettings.get("value") + "(" + scrapDataArrayList.get(j).getBuying() + ")" + " for " + saveSettings.get("currency"));
                                        }
                                    } else if (condition.equals("<")) {
                                        if (Double.parseDouble(scrapDataArrayList.get(j).getBuying()) < Double.parseDouble(saveSettings.get("value"))) {
                                            sendNotification("Buying Rates are " + saveSettings.get("condition") + " " + saveSettings.get("value") + "(" + scrapDataArrayList.get(j).getBuying() + ")" + " for " + saveSettings.get("currency"));
                                        }
                                    } else if (condition.equals("<=")) {
                                        if (Double.parseDouble(scrapDataArrayList.get(j).getBuying()) <= Double.parseDouble(saveSettings.get("value"))) {
                                            sendNotification("Buying Rates are " + saveSettings.get("condition") + " " + saveSettings.get("value") + "(" + scrapDataArrayList.get(j).getBuying() + ")" + " for " + saveSettings.get("currency"));
                                        }
                                    } else if (condition.equals("=")) {
                                        if (Double.parseDouble(scrapDataArrayList.get(j).getBuying()) == Double.parseDouble(saveSettings.get("value"))) {
                                            sendNotification("Buying Rates are " + saveSettings.get("condition") + " " + saveSettings.get("value") + "(" + scrapDataArrayList.get(j).getBuying() + ")" + " for " + saveSettings.get("currency"));
                                        }
                                    }
                                } else {
                                    if (condition.equals(">")) {
                                        if (Double.parseDouble(scrapDataArrayList.get(j).getSelling()) > Double.parseDouble(saveSettings.get("value"))) {
                                            sendNotification("Selling Rates are " + saveSettings.get("condition") + " " + saveSettings.get("value") + "(" + scrapDataArrayList.get(j).getSelling() + ")" + " for " + saveSettings.get("currency"));
                                        }
                                    } else if (condition.equals(">=")) {
                                        if (Double.parseDouble(scrapDataArrayList.get(j).getSelling()) >= Double.parseDouble(saveSettings.get("value"))) {
                                            sendNotification("Selling Rates are " + saveSettings.get("condition") + " " + saveSettings.get("value") + "(" + scrapDataArrayList.get(j).getSelling() + ")" + " for " + saveSettings.get("currency"));
                                        }
                                    } else if (condition.equals("<")) {
                                        if (Double.parseDouble(scrapDataArrayList.get(j).getSelling()) < Double.parseDouble(saveSettings.get("value"))) {
                                            sendNotification("Selling Rates are " + saveSettings.get("condition") + " " + saveSettings.get("value") + "(" + scrapDataArrayList.get(j).getSelling() + ")" + " for " + saveSettings.get("currency"));
                                        }
                                    } else if (condition.equals("<=")) {
                                        if (Double.parseDouble(scrapDataArrayList.get(j).getSelling()) <= Double.parseDouble(saveSettings.get("value"))) {
                                            sendNotification("Selling Rates are " + saveSettings.get("condition") + " " + saveSettings.get("value") + "(" + scrapDataArrayList.get(j).getSelling() + ")" + " for " + saveSettings.get("currency"));
                                        }
                                    } else if (condition.equals("=")) {
                                        if (Double.parseDouble(scrapDataArrayList.get(j).getSelling()) == Double.parseDouble(saveSettings.get("value"))) {
                                            sendNotification("Selling Rates are " + saveSettings.get("condition") + " " + saveSettings.get("value") + "(" + scrapDataArrayList.get(j).getSelling() + ")" + " for " + saveSettings.get("currency"));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                //endregion
            }
            //endregion

            //region cancel alarm
            else {
                progressDialog.show();
                intent = new Intent(RateNotificationActivity.this, RateReceiver.class);
                intent.setAction("RateReceiver");
                alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                    alarmIntent = PendingIntent.getBroadcast(RateNotificationActivity.this, 201, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_MUTABLE);
                }
                else{
                    alarmIntent = PendingIntent.getBroadcast(RateNotificationActivity.this, 201, intent, PendingIntent.FLAG_NO_CREATE);
                }

                if (alarmIntent != null && alarmMgr != null) {
                    alarmMgr.cancel(alarmIntent);
                }

                SharedPreferences prefs12 = getSharedPreferences("notification_setting", MODE_PRIVATE);
                if (prefs12.edit().putBoolean("alarm", false).commit()) {
                    progressDialog.hide();
                }

                switch1.setText("Off");
            }
            //endregion
        });
        //endregion

        //region save button handler
        button.setOnClickListener(v -> {
            progressDialog.show();
            if (etValue.getText().toString().isEmpty()) {
                progressDialog.hide();
                etValue.setError("Please enter value.");
            } else {
                try {
                    double myEnteredValue = Double.parseDouble(etValue.getText().toString());

                    String currency = spCurrency.getSelectedItem().toString();
                    String bs = spBuySell.getSelectedItem().toString();
                    String condition = spCondition.getSelectedItem().toString();
                    settings.put("currency", currency);
                    settings.put("type", bs);
                    settings.put("condition", condition);
                    settings.put("value", etValue.getText().toString());

                    Gson gson1 = new Gson();
                    String hashMapString = gson1.toJson(settings);

                    //save in shared prefs
                    SharedPreferences prefs1 = getSharedPreferences("notification_setting", MODE_PRIVATE);
                    if (prefs1.edit().putString("setting", hashMapString).commit()) {
                        progressDialog.hide();
                        AlertDialog.Builder builder = new AlertDialog.Builder(RateNotificationActivity.this, R.style.MyAlertDialogStyle);
                        builder.setMessage("Your settings saved successfully.");
                        builder.setPositiveButton("OK", (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        });
                        builder.setCancelable(true);
                        AlertDialog alertDialog = builder.create();
                        alertDialog.setOnShowListener(dialog -> {
                            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.darkGreen));
                        });

                        alertDialog.show();
                    }
                } catch (NumberFormatException e) {
                    progressDialog.hide();
                    etValue.setError("Invalid input (Allowing only digits and one decimal).");
                }
            }
        });
        //endregion
    }

    //region dismiss all the running dialogs
    @Override
    protected void onDestroy() {
        if(progressDialog != null){
            progressDialog.dismiss();
        }
        super.onDestroy();
    }
    //endregion

    //region function to dispatch focus from input when user clicks outside
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
    //endregion

    //region back-arrow handler
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region getting values to select the previous selected position in spinners like currency, buy-sell and condition.
    void positionSetter() {
        for (int i = 0; i < buySell.size(); i++) {
            String string = saveSettings.get("type");
            if (buySell.get(i).equals(string)) {
                buySellPosition = i;
            }
        }
        for (int j = 0; j < condition.size(); j++) {
            String string = saveSettings.get("condition");
            if (condition.get(j).equals(string)) {
                conditionPosition = j;
            }
        }
        for (int k = 0; k < currency.size(); k++) {
            String string = saveSettings.get("currency");
            if (currency.get(k).equals(string)) {
                currencyPosition = k;
            }
        }
    }
    //endregion

    //region notification generate function
    private void sendNotification(String message) {
        Intent intent = new Intent(this, NavigationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        }
        else{
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "MyChannel")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle("Rate Notifier")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MyChannel";
            String description = "Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("MyChannel", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NotificationID.getID(), builder.build());
    }
    //endregion

    //region Get condition operator using condition string
    String getCondition() {
        String s = "";
        if (saveSettings.get("condition").equals("Greater than")) {
            s = ">";
        }
        if (saveSettings.get("condition").equals("Greater than or equal to")) {
            s = ">=";
        }
        if (saveSettings.get("condition").equals("Less than")) {
            s = "<";
        }
        if (saveSettings.get("condition").equals("Less than or equal to")) {
            s = "<=";
        }
        if (saveSettings.get("condition").equals("Equal to")) {
            s = "=";
        }
        return s;
    }
    //endregion
}