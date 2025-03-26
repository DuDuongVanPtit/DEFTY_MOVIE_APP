package com.example.defty_movie_app.api;

import com.example.defty_movie_app.model.ApiResponse;
import com.example.defty_movie_app.model.LoginRequest;
import com.example.defty_movie_app.model.LoginResponse;
import com.example.defty_movie_app.model.RegisterRequest;
import com.example.defty_movie_app.model.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {
    @POST("api/v1/user/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("api/v1/user/auth/register")
    Call<ApiResponse<UserResponse>> register(@Body RegisterRequest request);
}
