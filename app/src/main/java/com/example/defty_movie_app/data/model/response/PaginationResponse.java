package com.example.defty_movie_app.data.model.response;

import java.util.List;

public class PaginationResponse<T> {
    private List<T> content;
    private long totalElements;

    public List<T> getContent() {
        return content;
    }

    public long getTotalElements() {
        return totalElements;
    }
}

