package com.example.defty_movie_app.data.remote;

import com.example.defty_movie_app.data.model.response.EpisodeResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface EpisodeUserService {
    @GET("api/v1/user/accessible/episode/first/video")
    Call<EpisodeResponse> getBanners(@Query("slug") String movieSlug);
}