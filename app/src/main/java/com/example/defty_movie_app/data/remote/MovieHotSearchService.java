package com.example.defty_movie_app.data.remote;

import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.request.MovieNameResponse;
import com.example.defty_movie_app.data.model.response.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MovieHotSearchService {
    @GET("api/v1/user/accessible/movie/hot-search")
    Call<ApiResponse<List<MovieNameResponse>>> getBanners();

    @GET("api/v1/user/accessible/movie/movie-search") // Adjust the endpoint as per your API
    Call<ApiResponse<List<Movie>>> searchMovies(
            @Query("title") String title
    );
}