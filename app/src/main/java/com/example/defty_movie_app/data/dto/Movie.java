package com.example.defty_movie_app.data.dto;

import static android.icu.util.UniversalTimeScale.toLong;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.List;

public class Movie {
    @SerializedName("title")
    private String title;
    @SerializedName("thumbnail")
    private String imageUrl;
    @SerializedName("slug")
    private String slug;
    private Long id;
    private String description;
    private String posterPath;
    private String backdropPath;
    @SerializedName("releaseDate")
    private String releaseDate;
    private String genre;
    private float rating;
    @SerializedName("membershipType")
    private Integer isPremium;
    private String contentType; // "limited_free", "top_10", "premium", etc.
    private String videoUrl;
    private int duration; // in minutes

    public Movie(String title, String imageUrl, String slug, Integer isPremium) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.slug = slug;
        this.isPremium = isPremium;
    }

    public String getTitle() { return title; }

    public String getImageUrl() { return imageUrl; }

    public String getSlug(){
        return slug;
    }

    // Default constructor
    public Movie() {
    }

    // Constructor with essential fields
    public Movie(Long id, String title, String posterPath, String genre, float rating) {
        this.id = id;
        this.title = title;
        this.posterPath = posterPath;
        this.genre = genre;
        this.rating = rating;
    }

    // Full constructor
    public Movie(Long id, String title, String description, String posterPath,
                 String backdropPath, String releaseDate, String genre,
                 float rating, Integer isPremium, String contentType,
                 String videoUrl, int duration) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.releaseDate = releaseDate;
        this.genre = genre;
        this.rating = rating;
        this.isPremium = isPremium;
        this.contentType = contentType;
        this.videoUrl = videoUrl;
        this.duration = duration;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public void setBackdropPath(String backdropPath) {
        this.backdropPath = backdropPath;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public Integer isPremium() {
        return isPremium;
    }

    public void setPremium(Integer premium) {
        isPremium = premium;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    @NonNull
    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", rating=" + rating +
                ", isPremium=" + isPremium +
                ", contentType='" + contentType + '\'' +
                '}';
    }

}