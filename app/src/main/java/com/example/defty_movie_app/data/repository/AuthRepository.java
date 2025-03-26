package com.example.defty_movie_app.data.repository;

import com.example.defty_movie_app.data.remote.AuthApiService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthRepository {
    private static AuthRepository instance;
    private final AuthApiService api;

    public AuthRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8088/") // Dùng localhost trên Android Emulator
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(AuthApiService.class);
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    public AuthApiService getApi() {
        return api;
    }
}
