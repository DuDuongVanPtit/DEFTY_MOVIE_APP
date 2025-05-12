package com.example.defty_movie_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {
    public static Context setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);

        // Lưu ngôn ngữ vào SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefs.edit().putString("app_lang", languageCode).apply();

        return context.createConfigurationContext(config);
    }

    public static Context loadLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("app_lang", "en");
        return setLocale(context, lang);
    }

    public static String getLanguage(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0).getLanguage();
    }

    public static Context wrap(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

}


