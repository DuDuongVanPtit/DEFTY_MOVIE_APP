package com.example.defty_movie_app.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.viewmodel.AuthViewModel;


public class LoginFragment extends Fragment {

    private EditText edtUsername, edtPassword;
    private Button btnLogin;
    private AuthViewModel loginViewModel;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.log_in_bottom_sheet, container, false);
        initViews(view);

        // Khởi tạo ViewModel
        loginViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Quan sát các LiveData
        loginViewModel.isLoginSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess) {
                goToHome();
                Toast.makeText(getContext(), "Login Successful!", Toast.LENGTH_SHORT).show();
            }
        });

        loginViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        setListeners(view);
        return view;
    }

    private void initViews(View view) {
        edtUsername = view.findViewById(R.id.edtUsername);
        edtPassword = view.findViewById(R.id.edtPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
    }

    private void setListeners(View view) {
        btnLogin.setOnClickListener(v -> {
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (validateInput(username, password)) {
                loginViewModel.loginUser(username, password);
            }
        });
    }

    private boolean validateInput(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            edtUsername.setError("Username is required");
            edtUsername.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Password is required");
            edtPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void goToHome() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentLayout, new HomeFragment())
                .addToBackStack(null)
                .commit();
    }
}
