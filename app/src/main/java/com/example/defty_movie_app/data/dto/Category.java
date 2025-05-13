package com.example.defty_movie_app.data.dto;

import java.util.List;

public class Category {
    List<Movie> movies;
    List<String> regions;
    List<String> paidCategories;
    List<Integer> releaseDates;
    List<String> categories;
    List<String> slugs;

    public Category(List<Movie> movies, List<String> regions, List<String> paidCategories, List<Integer> releaseDates, List<String> categories, List<String> slugs) {
        this.movies = movies;
        this.regions = regions;
        this.paidCategories = paidCategories;
        this.releaseDates = releaseDates;
        this.categories = categories;
        this.slugs = slugs;
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void setMovies(List<Movie> movies) {
        this.movies = movies;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }

    public List<String> getPaidCategories() {
        return paidCategories;
    }

    public void setPaidCategories(List<String> paidCategories) {
        this.paidCategories = paidCategories;
    }

    public List<Integer> getReleaseDates() {
        return releaseDates;
    }

    public void setReleaseDates(List<Integer> releaseDates) {
        this.releaseDates = releaseDates;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<String> getSlugs() {
        return slugs;
    }

    public void setSlugs(List<String> slugs) {
        this.slugs = slugs;
    }
}