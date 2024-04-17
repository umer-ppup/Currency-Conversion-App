package com.atrule.currencyconverter.interfaces;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface ApiService {
    String BASE_URL = "http://atrule.com/";
    @GET
    Call<String> getResponse(@Url String url);
}