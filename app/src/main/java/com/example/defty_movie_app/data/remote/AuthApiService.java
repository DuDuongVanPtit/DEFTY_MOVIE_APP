package com.example.defty_movie_app.data.remote;

import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.request.LoginRequest;
import com.example.defty_movie_app.data.model.response.LoginResponse;
import com.example.defty_movie_app.data.model.request.RegisterRequest;
import com.example.defty_movie_app.data.model.response.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {
    @POST("api/v1/user/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("api/v1/user/auth/register")
    Call<ApiResponse<UserResponse>> register(@Body RegisterRequest request);
}
