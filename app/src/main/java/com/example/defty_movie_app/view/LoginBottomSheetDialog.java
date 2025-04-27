package com.example.defty_movie_app.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.viewmodel.SelectedLoginViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class LoginBottomSheetDialog extends BottomSheetDialogFragment {
    private SelectedLoginViewModel loginViewModel;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_bottom_sheet_fragment, container, false);
        ImageButton btClose = view.findViewById(R.id.btClose);
        btClose.setOnClickListener(v -> dismiss());

        // Khởi tạo ViewModel
        loginViewModel = new ViewModelProvider(this).get(SelectedLoginViewModel.class);

        // Tìm button trong layout
        LinearLayout btnLoginWithPassword = view.findViewById(R.id.btnLoginPassword);

        // Gửi sự kiện tới ViewModel khi nhấn nút
        btnLoginWithPassword.setOnClickListener(v -> loginViewModel.onLoginWithPasswordClicked());

        // Lắng nghe sự kiện từ ViewModel
        loginViewModel.navigateToPasswordLogin.observe(getViewLifecycleOwner(), shouldNavigate -> {
            if (shouldNavigate) {
                // Đóng BottomSheet hiện tại
                dismiss();

                // Mở BottomSheet mới (Login with Password)
                LoginWithPasswordBottomSheetDialog newDialog = new LoginWithPasswordBottomSheetDialog();
                newDialog.show(getParentFragmentManager(), "LoginWithPassword");

                // Reset lại trạng thái navigation trong ViewModel
                loginViewModel.resetNavigation();
            }
        });

        return view;
    }
}
