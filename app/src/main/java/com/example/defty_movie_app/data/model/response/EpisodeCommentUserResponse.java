package com.example.defty_movie_app.data.model.response;

import com.google.gson.annotations.SerializedName;

public class EpisodeCommentUserResponse {

    @SerializedName("id") // Khớp với backend
    private Integer id; // Đổi từ userId

    @SerializedName("fullName") // Khớp với backend
    private String fullName; // Đổi từ userName

    @SerializedName("avatar") // Khớp với backend
    private String avatar; // Giữ tên avatar, có thể đổi thành userAvatar nếu muốn thống nhất

    @SerializedName("slug") // Khớp với backend
    private String slug;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}