package com.example.defty_movie_app.shared;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.Nullable;

public class UserManager {
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_FULLNAME = "user_fullname";
    private static final String KEY_TOKEN = "user_token";
    private static final String KEY_GENDER = "user_gender";
    private static final String KEY_DOB_TIMESTAMP = "user_dob_timestamp";
    private static final String KEY_PROFILE_IMAGE_PATH = "user_profile_image_path";

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

    public static void logout(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().clear().apply();
    }

    public static boolean updateFullName(Context context, String newFullName) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_FULLNAME, newFullName).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean updateGender(Context context, String newGender) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_GENDER, newGender).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getCurrentGender(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_GENDER, null);
    }

    public static boolean updateDateOfBirth(Context context, long newDobTimestamp) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putLong(KEY_DOB_TIMESTAMP, newDobTimestamp).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static long getDateOfBirthTimestamp(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_DOB_TIMESTAMP, 0L);
    }

    public static boolean updateProfileImagePath(Context context, @Nullable String imagePath) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            if (imagePath == null) {
                editor.remove(KEY_PROFILE_IMAGE_PATH);
            } else {
                editor.putString(KEY_PROFILE_IMAGE_PATH, imagePath);
            }
            editor.apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public static String getProfileImagePath(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PROFILE_IMAGE_PATH, null);
    }
}