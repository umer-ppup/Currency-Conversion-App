package com.atrule.currencyconverter.api;

import com.atrule.currencyconverter.interfaces.ApiService;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {
    public static RetrofitClient retrofit = null;
    public final ApiService apiInterface;

    public RetrofitClient() {
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor(new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY))
                .callTimeout(2, TimeUnit.MINUTES)
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl(ApiService.BASE_URL)
                .client(okHttpClient).build();

        apiInterface = retrofit.create(ApiService.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if(retrofit == null){
            retrofit = new RetrofitClient();
        }
        return retrofit;
    }

    public ApiService getMyApi() {
        return apiInterface;
    }
}