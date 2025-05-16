package com.example.defty_movie_app.data.model.request;

import com.google.gson.annotations.SerializedName;

public class MovieCommentRequest {

    @SerializedName("episodeId")
    private Integer episodeId;

    @SerializedName("content")
    private String content;

    @SerializedName("parentId") // Có thể null nếu là comment gốc
    private Integer parentId;

    private String username;

    // User ID thường được lấy từ token xác thực ở backend, không cần gửi từ client
    // @SerializedName("userId")
    // private Integer userId;

    public MovieCommentRequest(Integer episodeId, String content, Integer parentId, String username) {
        this.episodeId = episodeId;
        this.content = content;
        this.parentId = parentId;
        this.username = username;
    }

//    public MovieCommentRequest(Integer episodeId, String content, String username) {
//        this(episodeId, content, null, username); // Constructor cho comment gốc
//    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // Getters (Setters có thể không cần nếu chỉ dùng để gửi đi)
    public Integer getEpisodeId() {
        return episodeId;
    }

    public String getContent() {
        return content;
    }

    public Integer getParentId() {
        return parentId;
    }
}
