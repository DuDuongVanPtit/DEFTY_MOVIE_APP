package com.example.defty_movie_app.data.dto;

public class Language {
    private final String code;
    private final String displayName;
    private final int flagResId; // drawable resource

    public Language(String code, String displayName, int flagResId) {
        this.code = code;
        this.displayName = displayName;
        this.flagResId = flagResId;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public int getFlagResId() { return flagResId; }
}

