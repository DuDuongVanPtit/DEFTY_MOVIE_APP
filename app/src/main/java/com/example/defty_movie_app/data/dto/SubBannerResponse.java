package com.example.defty_movie_app.data.dto;

public class SubBannerResponse {
    private String description;
    private int numberOfChild;
    private String releaseDate;

    // Constructor mặc định
    public SubBannerResponse() {
    }

    // Constructor đầy đủ (tùy chọn, có thể thêm nếu cần)
    public SubBannerResponse(String description, int numberOfChild, String releaseDate) {
        this.description = description;
        this.numberOfChild = numberOfChild;
        this.releaseDate = releaseDate;
    }

    // Getters and Setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getNumberOfChild() {
        return numberOfChild;
    }

    public void setNumberOfChild(int numberOfChild) {
        this.numberOfChild = numberOfChild;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }
}