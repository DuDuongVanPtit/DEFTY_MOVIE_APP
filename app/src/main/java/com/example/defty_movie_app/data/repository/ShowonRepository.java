package com.example.defty_movie_app.data.repository;

import com.example.defty_movie_app.data.remote.ShowonApiService;
import retrofit2.Retrofit;

public class ShowonRepository {
    private static ShowonRepository instance;
    private final ShowonApiService api;

    private ShowonRepository() {
        Retrofit retrofit = ApiClient.getRetrofitInstance();
        api = retrofit.create(ShowonApiService.class);
    }

    public static synchronized ShowonRepository getInstance() {
        if (instance == null) {
            instance = new ShowonRepository();
        }
        return instance;
    }

    public ShowonApiService getApi() {
        return api;
    }
}
