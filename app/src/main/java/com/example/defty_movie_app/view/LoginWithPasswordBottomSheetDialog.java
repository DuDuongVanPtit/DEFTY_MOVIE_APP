package com.example.defty_movie_app.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.model.request.LoginRequest;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.LoginResponse;
import com.example.defty_movie_app.data.remote.AuthApiService;
import com.example.defty_movie_app.data.repository.AuthRepository;
import com.example.defty_movie_app.viewmodel.AuthViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginWithPasswordBottomSheetDialog extends BottomSheetDialogFragment {
    private EditText edtUsername, edtPassword;
    private Button btnLogin;
    private ImageButton btnBack, btnClose;
    private TextView tvSignUp;
    private AuthViewModel authViewModel;
    private String token;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.log_in_bottom_sheet, container, false);

        // Ánh xạ các thành phần UI
        edtUsername = view.findViewById(R.id.edtUsername);
        edtPassword = view.findViewById(R.id.edtPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnBack = view.findViewById(R.id.btnBack);
        btnClose = view.findViewById(R.id.btnClose);
        tvSignUp = view.findViewById(R.id.tvSignUp);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);


        // Xử lý sự kiện nút Back
        btnBack.setOnClickListener(v -> {
            dismiss();
            LoginBottomSheetDialog loginBottomSheetDialog = new LoginBottomSheetDialog();
            loginBottomSheetDialog.show(getParentFragmentManager(), "SignUpBottomSheet");
        });

        // Xử lý sự kiện nút Close
        btnClose.setOnClickListener(v -> {
            dismiss(); // Đóng bottom sheet
        });

        // Xử lý sự kiện nút Login
        btnLogin.setOnClickListener(v -> {
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Snackbar.make(view, "Please enter email and password", Snackbar.LENGTH_SHORT).show();
            } else {
                loginUser(username, password);
            }
        });

        // Xử lý sự kiện nút Sign Up (nếu có)
        if (tvSignUp != null) {
            tvSignUp.setOnClickListener(v -> {
                dismiss();
                SignUpBottomSheetDialog signUpDialog = new SignUpBottomSheetDialog();
                signUpDialog.show(getParentFragmentManager(), "SignUpBottomSheet");
            });
        }

        return view;
    }

    private void loginUser(String email, String password) {
        Snackbar.make(requireView(), "Logging in...", Snackbar.LENGTH_SHORT).show();

        AuthApiService apiService = AuthRepository.getInstance().getApi();
        LoginRequest loginRequest = new LoginRequest(email, password);

        Call<ApiResponse<LoginResponse>> call = apiService.login(loginRequest);
        call.enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body().getData();
                    token = loginResponse.getToken();
                    System.out.println(response);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.contentLayout, new ProfileFragment())
                            .addToBackStack(null)
                            .commit();
                    dismiss();
                    authViewModel.fetchUserInfo(token);
                } else {
                    Snackbar.make(requireView(), "Login Failed", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                Snackbar.make(requireView(), "Error: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}