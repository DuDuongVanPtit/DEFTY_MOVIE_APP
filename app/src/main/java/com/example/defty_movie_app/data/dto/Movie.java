package com.example.defty_movie_app.data.dto;

public class Movie {
    private String title;
    private String imageUrl;

    private String slug;

    public Movie(String title, String imageUrl, String slug) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.slug = slug;
    }

    public String getTitle() { return title; }

    public String getImageUrl() { return imageUrl; }

    public String getSlug(){
        return slug;
    }
}
