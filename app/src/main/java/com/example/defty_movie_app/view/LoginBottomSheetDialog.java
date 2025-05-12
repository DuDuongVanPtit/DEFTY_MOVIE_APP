package com.example.defty_movie_app.view;

import android.content.Intent;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class LoginBottomSheetDialog extends BottomSheetDialogFragment {

    private SelectedLoginViewModel loginViewModel;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_bottom_sheet_fragment, container, false);
        ImageButton btClose = view.findViewById(R.id.btClose);
        btClose.setOnClickListener(v -> dismiss());

        loginViewModel = new ViewModelProvider(this).get(SelectedLoginViewModel.class);

        LinearLayout btnLoginWithPassword = view.findViewById(R.id.btnLoginPassword);
        btnLoginWithPassword.setOnClickListener(v -> loginViewModel.onLoginWithPasswordClicked());

        loginViewModel.navigateToPasswordLogin.observe(getViewLifecycleOwner(), shouldNavigate -> {
            if (shouldNavigate) {
                dismiss();
                LoginWithPasswordBottomSheetDialog newDialog = new LoginWithPasswordBottomSheetDialog();
                newDialog.show(getParentFragmentManager(), "LoginWithPassword");
                loginViewModel.resetNavigation();
            }
        });

        // --- Google Sign-In ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // lấy từ strings.xml
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        View btnLoginGoogle = view.findViewById(R.id.btnGoogle);
        btnLoginGoogle.setOnClickListener(v -> signInWithGoogle());

        return view;
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

}

