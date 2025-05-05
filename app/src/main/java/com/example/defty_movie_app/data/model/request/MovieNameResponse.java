package com.example.defty_movie_app.data.model.request;

import com.example.defty_movie_app.data.dto.Movie;

public class MovieNameResponse {
    private String name;
    private String thumbnail;
    private String slug;

    // Default constructor
    public MovieNameResponse() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    // Convert to Movie for adapter compatibility
    public Movie toMovie() {
        return new Movie(name, thumbnail, slug);
    }
}