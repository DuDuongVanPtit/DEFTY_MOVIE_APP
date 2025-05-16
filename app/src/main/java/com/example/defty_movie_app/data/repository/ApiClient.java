package com.example.defty_movie_app.data.repository;

import com.example.defty_movie_app.config.AppConstants;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit;

//    private static final String BASE_URL = "http://10.0.2.2:8089/";
    static String domain = AppConstants.DOMAIN;
    private static final String BASE_URL = domain + ":8088/";


    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
