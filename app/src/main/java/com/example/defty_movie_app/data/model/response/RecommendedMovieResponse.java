package com.example.defty_movie_app.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RecommendedMovieResponse {
    @SerializedName("movieId")
    public int movieId;

    @SerializedName("similar_movies")
    public List<RecommendedMovie> similarMovies;

    public static class RecommendedMovie {
        @SerializedName("title")
        public String title;

        @SerializedName("slug")
        public String slug;

        @SerializedName("cover_image")
        public String coverImage;
    }
}
