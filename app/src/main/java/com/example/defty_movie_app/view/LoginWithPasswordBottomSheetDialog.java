package com.example.defty_movie_app.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.viewmodel.AuthViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

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

        edtUsername = view.findViewById(R.id.edtUsername);
        edtPassword = view.findViewById(R.id.edtPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnBack = view.findViewById(R.id.btnBack);
        btnClose = view.findViewById(R.id.btnClose);
        tvSignUp = view.findViewById(R.id.tvSignUp);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        btnBack.setOnClickListener(v -> {
            dismiss();
            LoginBottomSheetDialog loginBottomSheetDialog = new LoginBottomSheetDialog();
            loginBottomSheetDialog.show(getParentFragmentManager(), "SignUpBottomSheet");
        });

        btnClose.setOnClickListener(v -> {
            dismiss();
        });

        authViewModel.getLoginSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Snackbar.make(view, R.string.login_successful, Snackbar.LENGTH_LONG).show();
                authViewModel.getTokenLiveData().observe(getViewLifecycleOwner(), token -> {
                    if (token != null) {
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.contentLayout, new ProfileFragment())
                                .addToBackStack(null)
                                .commit();
                        dismiss();
                        authViewModel.fetchUserInfo(token, requireContext());
                    }
                });
            } else {
                String error = authViewModel.getErrorMessage().getValue();
                String message = (error != null) ? error :
                        (getString(R.string.login_failed));
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
            }
        });

        btnLogin.setOnClickListener(v -> {
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Snackbar.make(view, getString(R.string.please_enter_email_and_password), Snackbar.LENGTH_SHORT).show();
            } else {
                authViewModel.loginUser(username, password, requireContext());
                Toast.makeText(requireContext(), getString(R.string.login_successful), Toast.LENGTH_LONG).show();
            }
        });

        if (tvSignUp != null) {
            tvSignUp.setOnClickListener(v -> {
                dismiss();
                SignUpBottomSheetDialog signUpDialog = new SignUpBottomSheetDialog();
                signUpDialog.show(getParentFragmentManager(), "SignUpBottomSheet");
            });
        }
        return view;
    }

}