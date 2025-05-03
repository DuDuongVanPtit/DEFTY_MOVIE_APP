package com.example.defty_movie_app.data.dto;

import java.util.Map;

public class Banner {
    private int id;
    private String key;
    private String thumbnail;
    private String title;
    private String link;
    private String contentType;
    private int contentId;
    private int position;
    private int status;
    private String contentName;
    private String contentSlug;
    private SubBannerResponse subBannerResponse;
    private Map<String, String> bannerItems;

    public Banner() {
    }

    public Banner(int id, String key, String thumbnail, String title, String link,
                  String contentType, int contentId, int position, int status,
                  String contentName, String contentSlug,
                  SubBannerResponse subBannerResponse, Map<String, String> bannerItems) {
        this.id = id;
        this.key = key;
        this.thumbnail = thumbnail;
        this.title = title;
        this.link = link;
        this.contentType = contentType;
        this.contentId = contentId;
        this.position = position;
        this.status = status;
        this.contentName = contentName;
        this.contentSlug = contentSlug;
        this.subBannerResponse = subBannerResponse;
        this.bannerItems = bannerItems;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getContentId() {
        return contentId;
    }

    public void setContentId(int contentId) {
        this.contentId = contentId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getContentName() {
        return contentName;
    }

    public void setContentName(String contentName) {
        this.contentName = contentName;
    }

    public String getContentSlug() {
        return contentSlug;
    }

    public void setContentSlug(String contentSlug) {
        this.contentSlug = contentSlug;
    }

    public SubBannerResponse getSubBannerResponse() {
        return subBannerResponse;
    }

    public void setSubBannerResponse(SubBannerResponse subBannerResponse) {
        this.subBannerResponse = subBannerResponse;
    }

    public Map<String, String> getBannerItems() {
        return bannerItems;
    }

    public void setBannerItems(Map<String, String> bannerItems) {
        this.bannerItems = bannerItems;
    }
}