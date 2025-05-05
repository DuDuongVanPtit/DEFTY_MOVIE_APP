package com.example.defty_movie_app.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.viewmodel.AuthViewModel;

public class ProfileFragment extends Fragment {

    private AuthViewModel authViewModel;
    private TextView loginText;

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        loginText = view.findViewById(R.id.login_text);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Sự kiện nhấn vào loginText để mở LoginBottomSheetDialog
        loginText.setOnClickListener(v -> {
            LoginBottomSheetDialog loginDialog = new LoginBottomSheetDialog();
            loginDialog.show(requireActivity().getSupportFragmentManager(), "LoginBottomSheetDialog");
        });

        // Quan sát dữ liệu người dùng
        observeUserData();

        // Sự kiện nhấn vào mục Language
        LinearLayout languageItem = view.findViewById(R.id.language_item);
        languageItem.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LanguageActivity.class);
            startActivity(intent);
        });


        return view;
    }

    private void observeUserData() {
        authViewModel.getUserResponse().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                System.out.println("User data: " + user.getFullName());
                loginText.setText(user.getFullName());
                loginText.setClickable(true);
            } else {
                System.out.println("No user data available");
                loginText.setText("Login / Signup");
                loginText.setClickable(true);
            }
        });
    }
}