package com.example.defty_movie_app.data.model.response;


import com.example.defty_movie_app.data.dto.Movie;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.List;

public class ShowonResponse {
    private Integer id;
    private Integer position;
    private Integer contentId;
    private String contentType;
    private String contentName;
    private Integer status;
    private String note;
    private Object contentItems;

    public ShowonResponse(Integer id, Integer position, Integer contentId, String contentType, String contentName, Integer status, String note, Object contentItems) {
        this.id = id;
        this.position = position;
        this.contentId = contentId;
        this.contentType = contentType;
        this.contentName = contentName;
        this.status = status;
        this.note = note;
        this.contentItems = contentItems;
    }

    public ShowonResponse() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getContentId() {
        return contentId;
    }

    public void setContentId(Integer contentId) {
        this.contentId = contentId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentName() {
        return contentName;
    }

    public void setContentName(String contentName) {
        this.contentName = contentName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<Movie> getContentItems() {
        // Ép kiểu Object thành List<Map<String, Object>> rồi chuyển đổi từng Map thành đối tượng Movie
        if (contentItems instanceof List<?>) {
            List<?> items = (List<?>) contentItems;
            List<Movie> movies = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof LinkedTreeMap) {
                    LinkedTreeMap<?, ?> movieMap = (LinkedTreeMap<?, ?>) item;

                    String title = (String) movieMap.get("movieTitle");
                    String thumbnail = (String) movieMap.get("movieThumbnail");
                    String slug = (String) movieMap.get("slug");

                    Object membershipTypeObj = movieMap.get("membershipType");
                    Integer isPremium = null;

                    if (membershipTypeObj instanceof Double) {
                        isPremium = ((Double) membershipTypeObj).intValue();
                    } else if (membershipTypeObj instanceof Integer) {
                        isPremium = (Integer) membershipTypeObj;
                    }
                    movies.add(new Movie(title, thumbnail, slug, isPremium));
                }
            }
            return movies;
        }
        return new ArrayList<>();
    }

    public void setContentItems(Object contentItems) {
        this.contentItems = contentItems;
    }
}
