package com.example.defty_movie_app.data.repository;

import com.example.defty_movie_app.data.remote.AuthApiService;
import retrofit2.Retrofit;

public class AuthRepository {
    private static AuthRepository instance;
    private final AuthApiService api;

    private AuthRepository() {
        Retrofit retrofit = ApiClient.getRetrofitInstance();
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
