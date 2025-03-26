package com.example.defty_movie_app.data.model.response;

public class LoginResponse {
    private String token;
    private UserResponse user;

    public String getToken() {
        return token;
    }

    public UserResponse getUser() {
        return user;
    }
}
