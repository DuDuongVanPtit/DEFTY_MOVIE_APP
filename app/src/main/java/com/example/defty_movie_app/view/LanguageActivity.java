package com.example.defty_movie_app.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.LanguageAdapter;
import com.example.defty_movie_app.data.dto.Language;
import com.example.defty_movie_app.utils.LocaleHelper;
import com.example.defty_movie_app.viewmodel.LanguageViewModel;

import java.util.Arrays;
import java.util.List;

public class LanguageActivity extends AppCompatActivity {

    private LanguageViewModel viewModel;
    private String currentLanguageCode;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("app_lang", "en");
        Context context = LocaleHelper.wrap(newBase, lang);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_activity);

        currentLanguageCode = LocaleHelper.getLanguage(this);
        viewModel = new ViewModelProvider(this).get(LanguageViewModel.class);

        RecyclerView recyclerView = findViewById(R.id.language_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Language> languages = Arrays.asList(
                new Language("en", "English", R.drawable.ic_eng),
                new Language("vi", "Tiếng Việt", R.drawable.ic_vn)
        );

        LanguageAdapter adapter = new LanguageAdapter(languages, viewModel);
        recyclerView.setAdapter(adapter);
        viewModel.getSelectedLanguageCode().observe(this, code -> {
            LocaleHelper.setLocale(LanguageActivity.this, code);
            SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
            prefs.edit().putString("app_lang", code).apply();
        });

        viewModel.getSelectedLanguageCode().observe(this, code -> {
            if (!code.equals(currentLanguageCode)) {
                LocaleHelper.setLocale(LanguageActivity.this, code);
                restartApp();
            }
        });

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    private void restartApp() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }
}
