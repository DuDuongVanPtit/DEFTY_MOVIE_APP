package com.example.defty_movie_app.data.model.adapter;

public class CastCrew {
    private final String name;
    private final String role;
    private final String imageUrl;

    public CastCrew(String name, String role, String imageUrl) {
        this.name = name;
        this.role = role;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
