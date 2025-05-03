package com.example.defty_movie_app.data.remote;

import com.example.defty_movie_app.data.model.response.MovieDetailResponse;
import com.example.defty_movie_app.data.model.response.RecommendedMovieResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RecommenderServiceApi {
    @GET("/movie/recommended-for-you")
    Call<RecommendedMovieResponse> getRecommendedMovie(@Query("movieId") Integer movieId);
}
