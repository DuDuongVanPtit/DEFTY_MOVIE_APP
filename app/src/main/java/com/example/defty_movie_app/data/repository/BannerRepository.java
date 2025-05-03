package com.example.defty_movie_app.data.repository;

import com.example.defty_movie_app.data.remote.BannerApiService;

import retrofit2.Retrofit;

public class BannerRepository {
    private static BannerRepository instance;
    private final BannerApiService api;

    private BannerRepository() {
        Retrofit retrofit = ApiClient.getRetrofitInstance();
        api = retrofit.create(BannerApiService.class);
    }

    public static synchronized BannerRepository getInstance() {
        if (instance == null) {
            instance = new BannerRepository();
        }
        return instance;
    }

    public BannerApiService getApi() {
        return api;
    }
}