package com.example.defty_movie_app.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.defty_movie_app.R;

public class ProfileFragment extends Fragment {

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile, container, false);

        TextView loginText = view.findViewById(R.id.login_text);
        loginText.setOnClickListener(v -> {
            LoginBottomSheetDialog loginDialog = new LoginBottomSheetDialog();
            loginDialog.show(requireActivity().getSupportFragmentManager(), "LoginBottomSheetDialog");
        });

        return view;
    }
}
