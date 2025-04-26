package com.example.defty_movie_app.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.model.request.LoginRequest;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.LoginResponse;
import com.example.defty_movie_app.data.remote.AuthApiService;
import com.example.defty_movie_app.data.repository.AuthRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginWithPasswordBottomSheetDialog extends BottomSheetDialogFragment {
    private EditText edtUsername, edtPassword;
    private Button btnLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_login, container, false);

        edtUsername = view.findViewById(R.id.edtUsername);
        edtPassword = view.findViewById(R.id.edtPassword);
        btnLogin = view.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(username, password);
            }
        });

        return view;
    }

    private void loginUser(String email, String password) {
        Toast.makeText(getContext(), "Logging in...", Toast.LENGTH_SHORT).show();

        AuthApiService apiService = AuthRepository.getInstance().getApi();
        LoginRequest loginRequest = new LoginRequest(email, password);

        Call<ApiResponse<LoginResponse>> call = apiService.login(loginRequest);
        call.enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body().getData();
                    Toast.makeText(getContext(), "Login Success: " + loginResponse.getToken(), Toast.LENGTH_SHORT).show();
                    // Chuyển sang HomeActivity
                    Intent intent = new Intent(getContext(), ProfileFragment.class);
                    startActivity(intent);
                    dismiss(); // Đóng BottomSheetDialog sau khi đăng nhập thành công
                } else {
                    Toast.makeText(getContext(), "Login Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
