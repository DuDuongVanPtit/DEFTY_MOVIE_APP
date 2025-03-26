//package com.example.defty_movie_app.view;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.lifecycle.ViewModelProvider;
//
//import com.example.defty_movie_app.R;
//import com.example.defty_movie_app.viewmodel.AuthViewModel;
//
//public class RegisterActivity extends AppCompatActivity {
//    private AuthViewModel authViewModel;
//    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
//    private Button btnRegister, btnBackToLogin;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_register);
//
//        etUsername = findViewById(R.id.etUsername);
//        etEmail = findViewById(R.id.etEmail);
//        etPassword = findViewById(R.id.etPassword);
//        etConfirmPassword = findViewById(R.id.etConfirmPassword);
//        btnRegister = findViewById(R.id.btnRegister);
//        btnBackToLogin = findViewById(R.id.btnBackToLogin);
//
//        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
//
//        authViewModel.getRegisterResult().observe(this, response -> {
//            if (response != null && response.getStatus() == 201) {
//                Toast.makeText(this, "Register Successful", Toast.LENGTH_SHORT).show();
//                startActivity(new Intent(this, LoginActivity.class));
//                finish();
//            } else {
//                Toast.makeText(this, "Register Failed", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        btnRegister.setOnClickListener(v -> {
//            String username = etUsername.getText().toString();
//            String email = etEmail.getText().toString();
//            String password = etPassword.getText().toString();
//            String confirmPassword = etConfirmPassword.getText().toString();
//
//            if (!password.equals(confirmPassword)) {
//                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            authViewModel.register(username, email, password);
//        });
//
//        btnBackToLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
//    }
//}
