package com.example.defty_movie_app.data.model.response;

public class EpisodeResponse {
    public int status;
    public String message;
    public Episode data;
    public static class Episode {
        private Integer number;
        private String description;
        private String link;
        private String slug;

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
    }
}
