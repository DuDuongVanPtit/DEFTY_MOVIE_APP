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
        public List<Episode> episode;
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
    public static class Episode{
        private Integer id;
        private Integer number;
        private String description;
        private String thumbnail;
        private String link;
        private String slug;
        private Integer movieId;
        private Integer status;
        private String processedLink;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getNumber() {
            return number;
        }

        public void setNumber(Integer number) {
            this.number = number;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public Integer getMovieId() {
            return movieId;
        }

        public void setMovieId(Integer movieId) {
            this.movieId = movieId;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
        public String getProcessedLink() {
            return processedLink;
        }
        public void setProcessedLink(String processedLink) {
            this.processedLink = processedLink;
        }

    }
    public static class Category {
        public String name;
        public String slug;
    }
}

