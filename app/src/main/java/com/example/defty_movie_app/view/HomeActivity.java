package com.example.defty_movie_app.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.defty_movie_app.R;


public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        TextView loginText = findViewById(R.id.login_text);
        loginText.setOnClickListener(v -> {
            LoginBottomSheetDialog loginDialog = new LoginBottomSheetDialog();
            loginDialog.show(getSupportFragmentManager(), "LoginBottomSheetDialog");
        });

        Button btnWatch = findViewById(R.id.watch);

        btnWatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, WatchActivity.class);
                intent.putExtra("videoUrl", "https://your-video-link.mp4");
                startActivity(intent);
            }
        });

    }
}
