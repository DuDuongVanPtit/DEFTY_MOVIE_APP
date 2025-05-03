package com.example.defty_movie_app.data.model.response;

import java.util.List;

public class MovieDetailResponse {
    public int status;
    public String message;
    public Movie data;

    public static class Movie {
        public Integer id;
        public String title;
        public String rating;
        public String trailer;
        public String releaseDate;
        public int duration;
        public String description;
        public String coverImage;
        public String slug;
        public Director director;
        public List<Actor> actor;
        public List<Category> category;
    }

    public static class Director {
        public String name;
        public String thumbnail;
        public String slug;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }
    }
    public static class Actor {
        public String name;
        public String avatar;
        public String slug;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

    public static class Category {
        public String name;
        public String slug;
    }
}

