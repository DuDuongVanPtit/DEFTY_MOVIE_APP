package com.example.defty_movie_app.data.repository;

import com.example.defty_movie_app.data.remote.MovieHotSearchService;
import com.example.defty_movie_app.data.remote.ShowonApiService;

import retrofit2.Retrofit;

public class MovieHotSearchRepository {
    private static MovieHotSearchRepository instance;
    private final MovieHotSearchService api;

    private MovieHotSearchRepository() {
        Retrofit retrofit = ApiClient.getRetrofitInstance();
        api = retrofit.create(MovieHotSearchService.class);
    }

    public static synchronized MovieHotSearchRepository getInstance() {
        if (instance == null) {
            instance = new MovieHotSearchRepository();
        }
        return instance;
    }

    public MovieHotSearchService getApi() {
        return api;
    }
}
