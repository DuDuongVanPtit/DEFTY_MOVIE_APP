package com.example.defty_movie_app.data.model.request;

import com.google.gson.annotations.SerializedName;

public class MovieCommentRequest {

    @SerializedName("episodeId")
    private Integer episodeId;

    @SerializedName("content")
    private String content;

    @SerializedName("parentId") // Có thể null nếu là comment gốc
    private Integer parentId;

    // User ID thường được lấy từ token xác thực ở backend, không cần gửi từ client
    // @SerializedName("userId")
    // private Integer userId;

    public MovieCommentRequest(Integer episodeId, String content, Integer parentId) {
        this.episodeId = episodeId;
        this.content = content;
        this.parentId = parentId;
    }

    public MovieCommentRequest(Integer episodeId, String content) {
        this(episodeId, content, null); // Constructor cho comment gốc
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
