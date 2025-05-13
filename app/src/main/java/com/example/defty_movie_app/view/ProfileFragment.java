package com.example.defty_movie_app.view;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.shared.UserManager;
import com.example.defty_movie_app.viewmodel.AuthViewModel;

import java.io.File;

public class ProfileFragment extends Fragment {
    private AuthViewModel authViewModel;
    private TextView loginText;
    private ImageView avatar;
    private ActivityResultLauncher<Intent> personalActivityLauncher;

    public ProfileFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        personalActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Returned from PersonalActivity, result code: " + result.getResultCode());
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        LinearLayout login = view.findViewById(R.id.header);
        loginText = view.findViewById(R.id.login_text);
        avatar = view.findViewById(R.id.avatar);

        String fullName = UserManager.getFullName(requireContext());

        if (!fullName.isEmpty()) {
            loginText.setText(fullName);
            loadAvatarFromInternalStorage();
            loginText.setClickable(false);
        } else {
            loginText.setText(R.string.login_sign_up);
            avatar.setImageResource(R.drawable.ic_avatar);
            loginText.setClickable(true);
        }

        login.setOnClickListener(v -> {
            if (getContext() == null || !isAdded()) return;
            String currentFullName = UserManager.getFullName(requireContext());

            if (!currentFullName.isEmpty()) {
                Intent intent = new Intent(getContext(), PersonalActivity.class);
                personalActivityLauncher.launch(intent);
            } else {
                LoginBottomSheetDialog loginDialog = new LoginBottomSheetDialog();
                loginDialog.show(requireActivity().getSupportFragmentManager(), "LoginBottomSheetDialog");
            }
        });

        observeUserData();

        LinearLayout languageItem = view.findViewById(R.id.language_item);
        languageItem.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LanguageActivity.class);
            startActivity(intent);
        });

        LinearLayout feedbackItem = view.findViewById(R.id.feedback_item);
        feedbackItem.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FeedbackActivity.class);
            startActivity(intent);
        });

        LinearLayout settingItem = view.findViewById(R.id.setting_item);
        settingItem.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingActivity.class);
            startActivity(intent);
        });
        return view;
    }

    private void observeUserData() {
        authViewModel.getUserResponse().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                System.out.println("User data: " + user.getFullName());
                loginText.setText(user.getFullName());
                loadAvatarFromInternalStorage();
            } else {
                System.out.println("No user data available");
                loginText.setText(R.string.login_sign_up);
                loginText.setClickable(true);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProfileDisplay();
    }
    private void updateProfileDisplay() {
        if (getContext() == null || !isAdded()) {
            return;
        }
        String fullName = UserManager.getFullName(requireContext());
        if (loginText != null) {
            if (!fullName.isEmpty()) {
                loginText.setText(fullName);
                loginText.setClickable(false);
            } else {
                loginText.setText(R.string.login_sign_up);
                loginText.setClickable(true);
            }
        }
        loadAvatarFromInternalStorage();
    }
    private void loadAvatarFromInternalStorage() {
        String profileImagePath = UserManager.getProfileImagePath(requireContext());
        System.out.println("Loading avatar from: " + profileImagePath);
        if (profileImagePath != null && !profileImagePath.isEmpty()) {
            Glide.with(this)
                    .load(new File(profileImagePath))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(avatar);
        } else {
            avatar.setImageResource(R.drawable.ic_avatar);
        }
    }
}