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
import com.atrule.currencyconverter.classes.NotificationID;
import com.atrule.currencyconverter.classes.ScrapData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RateWorker extends Worker {
    //region variable declarations
    public Context myContext;
    String dateOnline;
    private ArrayList<ScrapData> scrapDataArrayList;
    private HashMap<String, String> saveSettings = new HashMap<>();
    //endregion

    public RateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        myContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        scrapDataArrayList = new ArrayList<>();
        fetchScrapData();
        return Result.success();
    }

    //region web scrapping function
    private void fetchScrapData(){
        SharedPreferences prefs = myContext.getSharedPreferences("notification_setting", MODE_PRIVATE);
        String storedHashMapString = prefs.getString("setting", "");
        java.lang.reflect.Type type = new TypeToken<HashMap<String, String>>(){}.getType();
        Gson gson = new Gson();
        if(!storedHashMapString.equals("")){
            saveSettings = gson.fromJson(storedHashMapString, type);
        }

        if(!saveSettings.isEmpty()){
            Call<String> stringCall = RetrofitClient.getInstance().getMyApi().getResponse(myContext.getResources().getString(R.string.open_rate_url));

            stringCall.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                    if(response.isSuccessful()){
                        String responseString = response.body();

                        if(responseString != null){
                            try{
                                //region web scraping code
                                Document doc = Jsoup.parse(responseString);
                                Elements table = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody");
                                int size = table.get(0).childrenSize();

                                Elements date = doc.select("body > table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td:nth-of-type(1) > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > p:nth-of-type(1)");
                                String stringDate = date.get(0).childNodes().get(1).toString().trim();
                                dateOnline = stringDate.substring(6, 22);

                                for(int i=2; i <= size; i++){
                                    Elements currency = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(1)");
                                    Elements symbol = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(2)");
                                    Elements buying = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(3)");
                                    Elements selling = doc.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > table > tbody > tr:nth-of-type(3) > td > div > div:nth-of-type(2) > table > tbody > tr > td:nth-of-type(2) > table:nth-of-type(2) > tbody > tr:nth-of-type("+i+") > td:nth-of-type(4)");

                                    ScrapData scrapData = new ScrapData();
                                    scrapData.setCurrency(currency.get(0).childNodes().get(1).toString().trim());
                                    String string = scrapData.getCurrency();
                                    string = string.substring(12).trim();
                                    scrapData.setCurrency(string);
                                    scrapData.setSymbol(symbol.get(0).childNodes().get(0).childNodes().get(0).toString().trim());
                                    scrapData.setBuying(buying.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setSelling(selling.get(0).childNodes().get(0).toString().trim());
                                    scrapData.setFlag(0);
                                    scrapDataArrayList.add(scrapData);
                                }
                                //endregion

                                //region show notification if required condition satisfy
                                for(int j = 0; j < scrapDataArrayList.size(); j++){
                                    String condition = getCondition();
                                    if(scrapDataArrayList.get(j).getSymbol().equalsIgnoreCase(saveSettings.get("currency"))){
                                        if(saveSettings.get("type").equals("Buying")){
                                            if(condition.equals(">")){
                                                if(Double.parseDouble(scrapDataArrayList.get(j).getBuying()) > Double.parseDouble(saveSettings.get("value"))){
                                                    sendNotification("Buying Rates are "+saveSettings.get("condition")+" "+saveSettings.get("value")+"("+scrapDataArrayList.get(j).getBuying()+")"+" for "+saveSettings.get("currency"));
                                                }
                                            }
                                            else if(condition.equals(">=")){
                                                if(Double.parseDouble(scrapDataArrayList.get(j).getBuying()) >= Double.parseDouble(saveSettings.get("value"))){
                                                    sendNotification("Buying Rates are "+saveSettings.get("condition")+" "+saveSettings.get("value")+"("+scrapDataArrayList.get(j).getBuying()+")"+" for "+saveSettings.get("currency"));
                                                }
                                            }
                                            else if(condition.equals("<")){
                                                if(Double.parseDouble(scrapDataArrayList.get(j).getBuying()) < Double.parseDouble(saveSettings.get("value"))){
                                                    sendNotification("Buying Rates are "+saveSettings.get("condition")+" "+saveSettings.get("value")+"("+scrapDataArrayList.get(j).getBuying()+")"+" for "+saveSettings.get("currency"));
                                                }
                                            }
                                            else if(condition.equals("<=")){
                                                if(Double.parseDouble(scrapDataArrayList.get(j).getBuying()) <= Double.parseDouble(saveSettings.get("value"))){
                                                    sendNotification("Buying Rates are "+saveSettings.get("condition")+" "+saveSettings.get("value")+"("+scrapDataArrayList.get(j).getBuying()+")"+" for "+saveSettings.get("currency"));
                                                }
                                            }
                                            else if(condition.equals("=")){
                                                if(Double.parseDouble(scrapDataArrayList.get(j).getBuying()) == Double.parseDouble(saveSettings.get("value"))){
                                                    sendNotification("Buying Rates are "+saveSettings.get("condition")+" "+saveSettings.get("value")+"("+scrapDataArrayList.get(j).getBuying()+")"+" for "+saveSettings.get("currency"));
                                                }
                                            }
                                        }
                                        else{
                                            if(condition.equals(">")){
                                                if(Double.parseDouble(scrapDataArrayList.get(j).getSelling()) > Double.parseDouble(saveSettings.get("value"))){
                                                    sendNotification("Selling Rates are "+saveSettings.get("condition")+" "+saveSettings.get("value")+"("+scrapDataArrayList.get(j).getSelling()+")"+" for "+saveSettings.get("currency"));
                                                }
                                            }
                                            else if(condition.equals(">=")){
                                                if(Double.parseDouble(scrapDataArrayList.get(j).getSelling()) >= Double.parseDouble(saveSettings.get("value"))){
                                                    sendNotification("Selling Rates are "+saveSettings.get("condition")+" "+saveSettings.get("value")+"("+scrapDataArrayList.get(j).getSelling()+")"+" for "+saveSettings.get("currency"));
                                                }
                                            }
                                            else if(condition.equals("<")){
                                                if(Double.parseDouble(scrapDataArrayList.get(j).getSelling()) < Double.parseDouble(saveSettings.get("value"))){
                                                    sendNotification("Selling Rates are "+saveSettings.get("condition")+" "+saveSettings.get("value")+"("+scrapDataArrayList.get(j).getSelling()+")"+" for "+saveSettings.get("currency"));
                                                }
                                            }
                                            else if(condition.equals("<=")){
                                                if(Double.parseDouble(scrapDataArrayList.get(j).getSelling()) <= Double.parseDouble(saveSettings.get("value"))){
                                                    sendNotification("Selling Rates are "+saveSettings.get("condition")+" "+saveSettings.get("value")+"("+scrapDataArrayList.get(j).getSelling()+")"+" for "+saveSettings.get("currency"));
                                                }
                                            }
                                            else if(condition.equals("=")){
                                                if(Double.parseDouble(scrapDataArrayList.get(j).getSelling()) == Double.parseDouble(saveSettings.get("value"))){
                                                    sendNotification("Selling Rates are "+saveSettings.get("condition")+" "+saveSettings.get("value")+"("+scrapDataArrayList.get(j).getSelling()+")"+" for "+saveSettings.get("currency"));
                                                }
                                            }
                                        }
                                    }
                                }
                                //endregion
                            }catch(Exception e){}
                        }
                    }
                }

                @Override
                public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {
                }
            });
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

    //region Get condition operator using condition string
    String getCondition(){
        String s = "";
        if(saveSettings.get("condition").equals("Greater than")){
            s = ">";
        }
        if(saveSettings.get("condition").equals("Greater than or equal to")){
            s = ">=";
        }
        if(saveSettings.get("condition").equals("Less than")){
            s = "<";
        }
        if(saveSettings.get("condition").equals("Less than or equal to")){
            s = "<=";
        }
        if(saveSettings.get("condition").equals("Equal to")){
            s = "=";
        }
        return s;
    }
    //endregion
}
