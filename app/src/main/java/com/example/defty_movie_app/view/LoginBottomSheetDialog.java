package com.example.defty_movie_app.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.viewmodel.LoginViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class LoginBottomSheetDialog extends BottomSheetDialogFragment {
    private LoginViewModel loginViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_bottom_sheet, container, false);

        // Khởi tạo ViewModel
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Tìm button trong layout
        LinearLayout btnLoginWithPassword = view.findViewById(R.id.btnLoginPassword);

        // Gửi sự kiện tới ViewModel khi nhấn nút
        btnLoginWithPassword.setOnClickListener(v -> loginViewModel.onLoginWithPasswordClicked());

        // Lắng nghe sự kiện từ ViewModel
        loginViewModel.navigateToPasswordLogin.observe(getViewLifecycleOwner(), shouldNavigate -> {
            if (shouldNavigate) {
                LoginWithPasswordBottomSheetDialog newDialog = new LoginWithPasswordBottomSheetDialog();
                newDialog.show(getParentFragmentManager(), "LoginWithPassword");
                loginViewModel.resetNavigation();
            }
        });

        return view;
    }
}
