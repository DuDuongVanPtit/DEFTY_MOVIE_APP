package com.example.defty_movie_app.data.dto;

import java.util.Objects;

public class DownloadedMovie {
    private Integer id;

    private String title;
    private String thumbnail;
    private String videoUrl;
    private String localFilePath; // Sẽ được cập nhật khi tải xong
    private long downloadDate;
    private String slug;
    private Integer number;

    // Trường mới
    private long downloadId; // ID từ DownloadManager
    private String downloadStatus; // Ví dụ: "PENDING", "DOWNLOADING", "COMPLETED", "FAILED"

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DOWNLOADING = "DOWNLOADING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PAUSED = "PAUSED";


    public DownloadedMovie() {
    }

    // Constructor có thể được điều chỉnh để bao gồm các trường mới nếu cần khi khởi tạo
    public DownloadedMovie(Integer id, String title, String thumbnail, String videoUrl, String slug,Integer number) {
        this.id = id;
        this.title = title;
        this.thumbnail = thumbnail;
        this.videoUrl = videoUrl;
        this.slug = slug;
        this.downloadDate = System.currentTimeMillis();
        this.downloadStatus = STATUS_PENDING; // Trạng thái ban đầu
        this.downloadId = -1; // Chưa có ID tải xuống
        this.localFilePath = null; // Chưa có đường dẫn file cục bộ
        this.number=number;
    }


    // Getters and Setters
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    public long getDownloadDate() {
        return downloadDate;
    }

    public void setDownloadDate(long downloadDate) {
        this.downloadDate = downloadDate;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public long getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }

    public String getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(String downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadedMovie that = (DownloadedMovie) o;
        // Một phim được coi là duy nhất dựa trên ID và slug của nó từ API gốc
        return id == that.id && Objects.equals(slug, that.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, slug);
    }
}
