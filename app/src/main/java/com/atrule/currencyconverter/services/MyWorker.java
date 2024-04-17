package com.atrule.currencyconverter.services;
import static android.content.Context.MODE_PRIVATE;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.atrule.currencyconverter.R;
import com.atrule.currencyconverter.activities.NavigationActivity;
import com.atrule.currencyconverter.api.RetrofitClient;
import com.atrule.currencyconverter.classes.Country;
import com.atrule.currencyconverter.classes.CurrencyRates;
import com.atrule.currencyconverter.classes.NotificationID;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyWorker extends Worker {
    //region variable declarations
    JSONObject storedJSONRates;
    HashMap<String, Double> jsonRates;
    final String CURRENCY_NAME_URL = "";
    ArrayList<Country> countries = new ArrayList<>();
    ArrayList<Country> sortedCountries = new ArrayList<>();
    String message;
    public Context myContext;
    //endregion

    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        myContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        countries = new ArrayList<>();
        sortedCountries = new ArrayList<>();
        notifyAboutRates();
        return Result.success();
    }

    //region check for rate and generate notification
    public void notifyAboutRates(){
        List<String> sortedCountriesNames = Arrays.asList(myContext.getResources().getStringArray(R.array.sorted_currency_names));
        SharedPreferences sharedPreferences = myContext.getSharedPreferences("Rates", MODE_PRIVATE);
        String rate = sharedPreferences.getString("jsonRate", "");
        if(!rate.equals("")){
            try {
                storedJSONRates = new JSONObject(rate);
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

                                    SharedPreferences sharedPreferences1 = myContext.getSharedPreferences("PinCities", MODE_PRIVATE);

                                    Call<String> stringCallAgain = RetrofitClient.getInstance().getMyApi().getResponse(CURRENCY_NAME_URL);
                                    stringCallAgain.enqueue(new Callback<String>() {
                                        @Override
                                        public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                                            if(response.isSuccessful()){
                                                String responseString = response.body();
                                                if(responseString != null){
                                                    if (!countries.isEmpty()) {
                                                        countries.clear();
                                                    }
                                                    if (!sortedCountries.isEmpty()) {
                                                        sortedCountries.clear();
                                                    }
                                                    JSONArray jsonArray;
                                                    try {
                                                        jsonArray = new JSONArray(responseString);

                                                        for (int i = 0; i < jsonArray.length(); i++) {
                                                            JSONObject mainObject = jsonArray.getJSONObject(i);
                                                            JSONArray flags = mainObject.getJSONArray("flags");
                                                            JSONObject nameJson = mainObject.getJSONObject("name");
                                                            if(mainObject.has("currencies")){
                                                                JSONObject currenciesJson = mainObject.getJSONObject("currencies");
                                                                Iterator<String> keysItr = currenciesJson.keys();
                                                                String key = "";
                                                                while(keysItr.hasNext()) {
                                                                    key = keysItr.next();
                                                                }
                                                                JSONObject currencyJson = currenciesJson.getJSONObject(key);
                                                                if(currencyJson.has("symbol")){
                                                                    Country country = new Country();
                                                                    country.setName(nameJson.getString("common"));
                                                                    country.setAlpha2Code(mainObject.getString("cca2"));
                                                                    country.setAlpha3Code(mainObject.getString("cca3"));
                                                                    country.setFlag(flags.get(0).toString());
                                                                    country.setCode(key);
                                                                    country.setSymbol(currencyJson.getString("symbol"));
                                                                    country.setPercent(0.0);
                                                                    country.setIncreaseDecrease(0);
                                                                    country.setChecked(false);
                                                                    if (jsonRates.containsKey(country.getCode().toLowerCase())) {
                                                                        country.setRate(Double.toString(jsonRates.get(country.getCode().toLowerCase())));
                                                                    } else {
                                                                        country.setRate("0");
                                                                    }
                                                                    countries.add(country);
                                                                }
                                                            }
                                                        }

                                                        for (int j = 0; j < sortedCountriesNames.size(); j++) {
                                                            for (int k = 0; k < countries.size(); k++) {
                                                                if (countries.get(k).getName().equals(sortedCountriesNames.get(j))) {
                                                                    sortedCountries.add(countries.get(k));
                                                                }
                                                            }
                                                        }

                                                        double currRate, prevRate;
                                                        for (int i = 0; i < sortedCountries.size(); i++) {
                                                            if (sharedPreferences1.contains(sortedCountries.get(i).getName())) {
                                                                if (sharedPreferences1.getBoolean(sortedCountries.get(i).getName(), false)) {
                                                                    if (!sortedCountries.get(i).getCode().equalsIgnoreCase("usd")) {
                                                                        currRate = jsonRates.get(sortedCountries.get(i).getCode().toLowerCase());
                                                                        prevRate = (double) storedJSONRates.get(sortedCountries.get(i).getCode().toLowerCase());

                                                                        if (currRate != prevRate) {
                                                                            if (currRate > prevRate) {
                                                                                message = "There is an increase in rates for " + sortedCountries.get(i).getName();
                                                                            } else {
                                                                                message = "There is a decrease in rates for " + sortedCountries.get(i).getName();
                                                                            }
                                                                        } else {
                                                                            message = "Rates are same for " + sortedCountries.get(i).getName();
                                                                        }
                                                                        sendNotification(message);
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        SharedPreferences shp = myContext.getSharedPreferences("Rates", MODE_PRIVATE);
                                                        SharedPreferences.Editor edt = shp.edit();
                                                        edt.putString("jsonRate", jsonRates.toString());
                                                        edt.apply();

                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        }

                                        @Override
                                        public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {

                                        }
                                    });
                                }
                                catch (Exception e) {

                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {

                    }
                });

            } catch (JSONException e) {

            }
        }
    }
    //endregion

    //region notification generate function
    private void sendNotification(String message) {
        Intent intent = new Intent(myContext, NavigationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            pendingIntent = PendingIntent.getActivity(myContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        }
        else{
            pendingIntent = PendingIntent.getActivity(myContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(myContext, "MyChannel")
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
            NotificationManager notificationManager = myContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(myContext);
        notificationManager.notify(NotificationID.getID(), builder.build());
    }
    //endregion
}
