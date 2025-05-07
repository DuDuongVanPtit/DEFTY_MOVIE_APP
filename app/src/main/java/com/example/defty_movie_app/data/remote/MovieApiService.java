package com.example.defty_movie_app.data.remote;

import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.request.MovieNameResponse;
import com.example.defty_movie_app.data.model.response.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MovieApiService {
    @GET("api/v1/user/accessible/movie/hot-search")
    Call<ApiResponse<List<MovieNameResponse>>> getMovies();

    @GET("api/v1/user/accessible/movie/movie-search")
    Call<ApiResponse<List<MovieNameResponse>>> searchMovies(
         @Query("title") String title
    );
}