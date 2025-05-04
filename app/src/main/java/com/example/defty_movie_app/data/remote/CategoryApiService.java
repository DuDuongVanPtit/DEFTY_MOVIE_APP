package com.example.defty_movie_app.data.remote;

import com.example.defty_movie_app.data.dto.Category;
import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.response.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CategoryApiService {
    @GET("api/v1/user/accessible/category")
    Call<ApiResponse<Category>> getCategories();

    @GET("api/v1/user/accessible/category/search")
    Call<ApiResponse<List<Movie>>> searchMovie(@Query("category") String category,
                                               @Query("region") String region,
                                               @Query("releaseYear") Integer releaseYear,
                                               @Query("paidCategory") Integer paidCategory);
}
