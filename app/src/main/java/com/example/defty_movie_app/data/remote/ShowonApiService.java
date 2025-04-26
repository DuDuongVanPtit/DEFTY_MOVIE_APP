package com.example.defty_movie_app.data.remote;

import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.PaginationResponse;
import com.example.defty_movie_app.data.model.response.ShowonResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ShowonApiService {
    @GET("api/v1/user/accessible/show-on/all")
    Call<ApiResponse<PaginationResponse<ShowonResponse>>> getAllShowons(
            @Query("page") int page,
            @Query("size") int size,
            @Query("contentType") String contentType,
            @Query("contentName") String contentName,
            @Query("status") Integer status
    );
}
