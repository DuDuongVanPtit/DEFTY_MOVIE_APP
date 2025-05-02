package com.example.defty_movie_app.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.viewmodel.AuthViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

public class SignUpBottomSheetDialog extends BottomSheetDialogFragment {

    private EditText edtEmail, edtUsername, edtPassword, edtFullName;
    private AppCompatButton btnSignUp;
    private ImageButton btnBack, btnClose;
    private TextView tvLogin;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sign_up_bottom_sheet, container, false);

        edtEmail = view.findViewById(R.id.edtEmail);
        edtUsername = view.findViewById(R.id.edtUsername);
        edtPassword = view.findViewById(R.id.edtPassword);
        edtFullName = view.findViewById(R.id.edtFullName);
        btnSignUp = view.findViewById(R.id.btnSignUp);
        btnBack = view.findViewById(R.id.btnBack1);
        btnClose = view.findViewById(R.id.btnClose1);
        tvLogin = view.findViewById(R.id.tvLogin);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        authViewModel.isSignUpSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                showSnackbar("Registration successful!");
                dismiss();
                LoginWithPasswordBottomSheetDialog loginDialog = new LoginWithPasswordBottomSheetDialog();
                loginDialog.show(getParentFragmentManager(), "LoginBottomSheet");
            }
        });

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> showSnackbar(message));

        btnBack.setOnClickListener(v -> {
            dismiss();
            LoginBottomSheetDialog loginBottomSheetDialog = new LoginBottomSheetDialog();
            loginBottomSheetDialog.show(getParentFragmentManager(), "SignUpBottomSheet");
        });

        btnClose.setOnClickListener(v -> dismiss());

        btnSignUp.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String fullName = edtFullName.getText().toString().trim();

            if (validateInputs(email, username, password, fullName)) {
                authViewModel.signUpUser(email, username, password, fullName);
            }
        });

        tvLogin.setOnClickListener(v -> {
            dismiss();
            LoginWithPasswordBottomSheetDialog loginDialog = new LoginWithPasswordBottomSheetDialog();
            loginDialog.show(getParentFragmentManager(), "LoginBottomSheet");
        });

        return view;
    }

    private boolean validateInputs(String email, String username, String password, String fullName) {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showSnackbar("Please enter a valid email");
            return false;
        }

        if (username.isEmpty() || username.length() < 3) {
            showSnackbar("Username must be at least 3 characters");
            return false;
        }

        if (password.isEmpty() || password.length() < 8) {
            showSnackbar("Password must be at least 6 characters");
            return false;
        }

        if (fullName.isEmpty()) {
            showSnackbar("Please enter your full name");
            return false;
        }

        return true;
    }

    private void showSnackbar(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
    }
}

