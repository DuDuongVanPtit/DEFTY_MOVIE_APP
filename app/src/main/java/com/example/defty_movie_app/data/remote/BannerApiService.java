package com.example.defty_movie_app.data.remote;

import com.example.defty_movie_app.data.dto.Banner;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.PaginationResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BannerApiService {
    @GET("api/v1/user/accessible/banner/all")
    Call<ApiResponse<PaginationResponse<Banner>>> getBanners(
            @Query("page") int page,
            @Query("size") int size,
            @Query("title") String title,
            @Query("status") Integer status
    );
}
