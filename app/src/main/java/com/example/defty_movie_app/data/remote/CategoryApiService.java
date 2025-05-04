package com.example.defty_movie_app.data.remote;

import com.example.defty_movie_app.data.dto.Category;
import com.example.defty_movie_app.data.model.response.ApiResponse;

import retrofit2.Call;
import retrofit2.http.GET;

public interface CategoryApiService {
    @GET("api/v1/user/accessible/category")
    Call<ApiResponse<Category>> getCategories();
}
