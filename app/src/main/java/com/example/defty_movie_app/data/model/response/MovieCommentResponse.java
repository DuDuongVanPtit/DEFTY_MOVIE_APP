package com.example.defty_movie_app.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MovieCommentResponse {

    @SerializedName("id")
    private Integer id;

    @SerializedName("content")
    private String content;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("replyTo")
    private String replyTo; // Có thể null

    @SerializedName("parentCommentId")
    private Integer parentCommentId; // Có thể null

    @SerializedName("user")
    private EpisodeCommentUserResponse user;

    @SerializedName("reactions")
    private List<CommentReactionResponse> reactions; // Có thể null hoặc rỗng

    @SerializedName("totalReply")
    private Integer totalReply;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public Integer getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Integer parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public EpisodeCommentUserResponse getUser() {
        return user;
    }

    public void setUser(EpisodeCommentUserResponse user) {
        this.user = user;
    }

    public List<CommentReactionResponse> getReactions() {
        return reactions;
    }

    public void setReactions(List<CommentReactionResponse> reactions) {
        this.reactions = reactions;
    }

    public Integer getTotalReply() {
        return totalReply == null ? 0 : totalReply;
    }

    public void setTotalReply(Integer totalReply) {
        this.totalReply = totalReply;
    }

    // --- Các phương thức tiện ích ---

    public String getUserName() {
        return (user != null && user.getFullName() != null) ? user.getFullName() : "Người dùng ẩn danh";
    }

    public String getUserAvatar() {
        return (user != null) ? user.getAvatar() : null;
    }

    // Đã xóa getLikeCount() vì không rõ ràng từ cấu trúc DTO backend và JSON mẫu.
    // Nếu backend cung cấp trường likeCount riêng, hãy thêm getter cho nó.
    // Hoặc nếu 'reactions' được dùng để thể hiện like, bạn cần logic tùy chỉnh ở đây hoặc trong Adapter.
}
