package com.example.defty_movie_app.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.utils.LocaleHelper;
import com.example.defty_movie_app.viewmodel.FeedbackViewModel;

public class FeedbackActivity extends AppCompatActivity {
    private EditText editTextMessage;
    private AppCompatButton btnFeedback;
    private FeedbackViewModel viewModel;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("app_lang", "en");
        Context context = LocaleHelper.wrap(newBase, lang);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        initViews();
        initViewModel();
        setupListeners();
        observeViewModel();
    }

    private void initViews() {
        editTextMessage = findViewById(R.id.editTextMessage);
        btnFeedback = findViewById(R.id.btnFeedback);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(FeedbackViewModel.class);
    }

    private void setupListeners() {
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        btnFeedback.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                viewModel.sendFeedback(message);
            } else {
                Toast.makeText(this, getString(R.string.error_empty_feedback), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            btnFeedback.setEnabled(!isLoading);
            if (isLoading) {
                btnFeedback.setText(getString(R.string.send));
            } else {
                btnFeedback.setText(getString(R.string.feedback));
            }
        });

        viewModel.getSendSuccessful().observe(this, successful -> {
            if (successful != null && successful) {
                Toast.makeText(this, getString(R.string.feedback_sent_successfully), Toast.LENGTH_SHORT).show();
                editTextMessage.setText("");
                editTextMessage.postDelayed(this::finish, 1000);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}