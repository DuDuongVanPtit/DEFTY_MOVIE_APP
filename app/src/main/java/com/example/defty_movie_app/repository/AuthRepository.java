package com.example.defty_movie_app.repository;

import com.example.defty_movie_app.api.AuthApiService;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthRepository {
    private AuthApiService api;

    public AuthRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8088/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(AuthApiService.class);
    }

    public AuthApiService getApi() {
        return api;
    }
}
