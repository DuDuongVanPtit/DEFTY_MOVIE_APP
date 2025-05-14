package com.example.defty_movie_app.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PageableResponse<T> {

    @SerializedName("content") // Tên trường chứa danh sách dữ liệu (thường là "content" trong Spring Page)
    private List<T> content;

    @SerializedName("pageable")
    private PageableInfo pageable; // Thông tin phân trang

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("totalElements")
    private long totalElements;

    @SerializedName("last")
    private boolean last;

    @SerializedName("first")
    private boolean first;

    @SerializedName("size")
    private int size;

    @SerializedName("number") // Số trang hiện tại (thường bắt đầu từ 0)
    private int number;

    @SerializedName("numberOfElements")
    private int numberOfElements; // Số phần tử trong trang hiện tại

    @SerializedName("empty")
    private boolean empty;

    // Getters and Setters
    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public PageableInfo getPageable() {
        return pageable;
    }

    public void setPageable(PageableInfo pageable) {
        this.pageable = pageable;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public void setNumberOfElements(int numberOfElements) {
        this.numberOfElements = numberOfElements;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    // Lớp con cho thông tin "pageable" (nếu API trả về chi tiết)
    public static class PageableInfo {
        @SerializedName("sort")
        private SortInfo sort;
        @SerializedName("offset")
        private long offset;
        @SerializedName("pageNumber")
        private int pageNumber;
        @SerializedName("pageSize")
        private int pageSize;
        @SerializedName("paged")
        private boolean paged;
        @SerializedName("unpaged")
        private boolean unpaged;

        // Getters and Setters for PageableInfo
    }

    // Lớp con cho thông tin "sort" (nếu API trả về chi tiết)
    public static class SortInfo {
        @SerializedName("empty")
        private boolean empty;
        @SerializedName("sorted")
        private boolean sorted;
        @SerializedName("unsorted")
        private boolean unsorted;

        // Getters and Setters for SortInfo
    }
}
