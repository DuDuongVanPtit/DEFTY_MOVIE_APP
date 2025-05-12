package com.example.defty_movie_app.shared;

import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_FULLNAME = "user_fullname";
    private static final String KEY_TOKEN = "user_token";

    public static void saveUser(Context context, String email, String fullname, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_FULLNAME, fullname)
                .putString(KEY_TOKEN, token)
                .apply();
    }

    public static String getEmail(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_EMAIL, "");
    }
    public static String getFullName(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_FULLNAME, "");
    }
    public static String getToken(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, "");
    }
    public static void logout(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        preferences.edit().clear().apply();
    }

}
