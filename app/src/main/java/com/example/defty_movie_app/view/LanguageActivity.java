package com.example.defty_movie_app.view;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.LanguageAdapter;
import com.example.defty_movie_app.data.dto.Language;
import com.example.defty_movie_app.viewmodel.LanguageViewModel;

import java.util.Arrays;
import java.util.List;

public class LanguageActivity extends AppCompatActivity {

    private LanguageViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_activity);

        viewModel = new ViewModelProvider(this).get(LanguageViewModel.class);

        // Thiết lập RecyclerView
        RecyclerView recyclerView = findViewById(R.id.language_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Language> languages = Arrays.asList(
                new Language("en", "English", R.drawable.ic_eng),
                new Language("vi", "Tiếng Việt", R.drawable.ic_vn)
        );

        LanguageAdapter adapter = new LanguageAdapter(languages, viewModel);
        recyclerView.setAdapter(adapter);

        viewModel.getSelectedLanguageCode().observe(this, code -> {
            Toast.makeText(this, "Language selected: " + code, Toast.LENGTH_SHORT).show();
        });

        // Xử lý nút back
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

}
