package com.example.defty_movie_app.data.model.response;

import com.google.gson.annotations.SerializedName;

public class SimpleResponse<T> {

    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data; // Trường dữ liệu generic, có thể là Integer, String, Boolean, hoặc null

    // Getters
    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    // Setters (tùy chọn, có thể không cần nếu chỉ dùng để nhận phản hồi)
    public void setStatus(int status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }

    // (Tùy chọn) Constructor nếu bạn cần tạo đối tượng này ở client
    public SimpleResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public SimpleResponse() {
    }
}