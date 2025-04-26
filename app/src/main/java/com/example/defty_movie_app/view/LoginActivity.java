package com.example.defty_movie_app.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.model.request.LoginRequest;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.LoginResponse;
import com.example.defty_movie_app.data.remote.AuthApiService;
import com.example.defty_movie_app.data.repository.AuthRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(view -> {
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            } else {
                // Xử lý đăng nhập (Gọi API)
                loginUser(username, password);
            }
        });
    }

    private void loginUser(String email, String password) {
        Toast.makeText(LoginActivity.this, "Logging in...", Toast.LENGTH_SHORT).show();

        AuthApiService apiService = AuthRepository.getInstance().getApi();
        LoginRequest loginRequest = new LoginRequest(email, password);

        Call<ApiResponse<LoginResponse>> call = apiService.login(loginRequest);
        call.enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body().getData();
                    Toast.makeText(LoginActivity.this, "Login Success: " + loginResponse.getToken(), Toast.LENGTH_SHORT).show();

                    // Chuyển sang màn hình chính
                    Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
