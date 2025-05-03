package com.example.defty_movie_app.data.remote;

import com.example.defty_movie_app.data.model.request.SignUpRequest;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.request.LoginRequest;
import com.example.defty_movie_app.data.model.response.EpisodeResponse;
import com.example.defty_movie_app.data.model.response.LoginResponse;
import com.example.defty_movie_app.data.model.request.RegisterRequest;
import com.example.defty_movie_app.data.model.response.MovieDetailResponse;
import com.example.defty_movie_app.data.model.response.SignUpResponse;
import com.example.defty_movie_app.data.model.response.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AuthApiService {
    @POST("api/v1/user/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("api/v1/user/auth/register")
    Call<ApiResponse<SignUpResponse>> signUp(@Body SignUpRequest request);

    @GET("api/v1/user/auth/check-account-token")
    Call<ApiResponse<UserResponse>> checkAccount(@Query("token") String token);
    Call<ApiResponse<UserResponse>> register(@Body RegisterRequest request);

    @GET("api/v1/user/accessible/episode")
    Call<MovieDetailResponse> getMovieDetail(@Query("slug") String slug);

    @GET("api/v1/user/accessible/episode/video")
    Call<EpisodeResponse> getEpisode(@Query("slugEpisode") String slug);
}
