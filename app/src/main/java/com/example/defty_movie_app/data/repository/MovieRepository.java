package com.example.defty_movie_app.data.repository;

import com.example.defty_movie_app.data.remote.MovieApiService;

import retrofit2.Retrofit;

public class MovieRepository {
    private static MovieRepository instance;
    private final MovieApiService api;

    private MovieRepository() {
        Retrofit retrofit = ApiClient.getRetrofitInstance();
        api = retrofit.create(MovieApiService.class);
    }

    public static synchronized MovieRepository getInstance() {
        if (instance == null) {
            instance = new MovieRepository();
        }
        return instance;
    }

    public MovieApiService getApi() {
        return api;
    }
}
