package com.example.defty_movie_app.data.model.response;

import com.google.gson.annotations.SerializedName;

public class CommentReactionResponse {

    // Các trường cũ đã bị xóa: reactionId, userId, reactionType

    @SerializedName("content") // Mới, khớp với backend
    private String content;

    @SerializedName("createdDate") // Mới, khớp với backend
    private String createdDate; // Giữ là String để SimpleDateFormat dễ xử lý

    @SerializedName("user") // Mới, khớp với backend
    private EpisodeCommentUserResponse user; // Thông tin người tạo reaction

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public EpisodeCommentUserResponse getUser() {
        return user;
    }

    public void setUser(EpisodeCommentUserResponse user) {
        this.user = user;
    }
}