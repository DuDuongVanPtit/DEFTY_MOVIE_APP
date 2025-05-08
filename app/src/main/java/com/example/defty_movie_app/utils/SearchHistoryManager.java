package com.example.defty_movie_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.LinkedHashSet; // To maintain insertion order and uniqueness

public class SearchHistoryManager {

    private static final String PREFS_NAME = "SearchHistoryPrefs";
    private static final String HISTORY_KEY = "search_history";
    private static final int MAX_HISTORY_SIZE = 10; // Giới hạn số lượng mục lịch sử

    private SharedPreferences sharedPreferences;
    private Gson gson;

    public SearchHistoryManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<String> getSearchHistory() {
        String jsonHistory = sharedPreferences.getString(HISTORY_KEY, null);
        if (jsonHistory == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        List<String> history = gson.fromJson(jsonHistory, type);
        return history != null ? history : new ArrayList<>();
    }

    public void addSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        String trimmedQuery = query.trim();
        List<String> history = getSearchHistory();

        // Sử dụng LinkedHashSet để tự động xử lý việc trùng lặp và duy trì thứ tự
        // Thêm vào đầu danh sách (hoặc đảm bảo nó ở đầu nếu đã tồn tại)
        LinkedHashSet<String> historySet = new LinkedHashSet<>();
        historySet.add(trimmedQuery); // Thêm mục mới nhất lên đầu
        historySet.addAll(history); // Thêm các mục cũ

        // Chuyển lại thành List và giới hạn kích thước
        ArrayList<String> updatedHistory = new ArrayList<>(historySet);
        while (updatedHistory.size() > MAX_HISTORY_SIZE) {
            updatedHistory.remove(updatedHistory.size() - 1); // Xóa mục cũ nhất
        }

        saveSearchHistory(updatedHistory);
    }

    private void saveSearchHistory(List<String> history) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String jsonHistory = gson.toJson(history);
        editor.putString(HISTORY_KEY, jsonHistory);
        editor.apply();
    }

    public void clearSearchHistory() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(HISTORY_KEY);
        editor.apply();
    }
}
