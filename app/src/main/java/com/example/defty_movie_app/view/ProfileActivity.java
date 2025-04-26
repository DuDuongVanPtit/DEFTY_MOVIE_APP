package com.example.defty_movie_app.view;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.defty_movie_app.R;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView loginText = findViewById(R.id.login_text);
        loginText.setOnClickListener(v -> {
            LoginBottomSheetDialog loginDialog = new LoginBottomSheetDialog();
            loginDialog.show(getSupportFragmentManager(), "LoginBottomSheetDialog");
        });
    }
}
