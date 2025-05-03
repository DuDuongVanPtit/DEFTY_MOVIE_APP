package com.example.defty_movie_app.data.repository;

import com.example.defty_movie_app.data.remote.RecommenderServiceApi;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CallRecommender {
    private static CallRecommender instance;
    private RecommenderServiceApi api;

    public CallRecommender() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5556/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(RecommenderServiceApi.class);
    }

    public static synchronized CallRecommender getInstance() {
        if (instance == null) {
            instance = new CallRecommender();
        }
        return instance;
    }

    public RecommenderServiceApi getApi() {
        return api;
    }
}
