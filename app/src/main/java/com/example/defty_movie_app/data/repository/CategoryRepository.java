package com.example.defty_movie_app.data.repository;

import com.example.defty_movie_app.data.remote.CategoryApiService;

import retrofit2.Retrofit;

public class CategoryRepository {
    private static CategoryRepository instance;
    private final CategoryApiService api;

    private CategoryRepository() {
        Retrofit retrofit = ApiClient.getRetrofitInstance();
        api = retrofit.create(CategoryApiService.class);
    }

    public static synchronized CategoryRepository getInstance() {
        if (instance == null) {
            instance = new CategoryRepository();
        }
        return instance;
    }

    public CategoryApiService getApi() {
        return api;
    }
}
