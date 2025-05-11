package com.example.defty_movie_app.data.dto;

public class SettingItem {
    private String key;
    private String title;
    private String summary;
    private String currentValue;
    private boolean isSelectable;
    private SettingItemType type;

    public enum SettingItemType {
        LIST_PREFERENCE,
        ACTION_PREFERENCE,
        INFO_PREFERENCE
    }

    public SettingItem(String key, String title, String summary, String currentValue, boolean isSelectable, SettingItemType type) {
        this.key = key;
        this.title = title;
        this.summary = summary;
        this.currentValue = currentValue;
        this.isSelectable = isSelectable;
        this.type = type;
    }

    public String getKey() { return key; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCurrentValue() { return currentValue; }
    public void setCurrentValue(String currentValue) { this.currentValue = currentValue; }
    public boolean isSelectable() { return isSelectable; }
    public SettingItemType getType() { return type; }

}
