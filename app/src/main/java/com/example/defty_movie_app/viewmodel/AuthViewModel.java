package com.example.defty_movie_app.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.defty_movie_app.data.model.request.LoginRequest;
import com.example.defty_movie_app.data.model.request.SignUpRequest;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.LoginResponse;
import com.example.defty_movie_app.data.model.response.SignUpResponse;
import com.example.defty_movie_app.data.remote.AuthApiService;
import com.example.defty_movie_app.data.repository.AuthRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends ViewModel {

    private AuthApiService apiService;

    // LiveData để theo dõi kết quả đăng nhập và đăng ký
    private MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private MutableLiveData<Boolean> signUpSuccess = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthViewModel() {
        apiService = AuthRepository.getInstance().getApi();
    }

    // Getter để truy cập LiveData
    public LiveData<Boolean> isLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<Boolean> isSignUpSuccess() {
        return signUpSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Phương thức đăng nhập
    public void loginUser(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);

        apiService.login(loginRequest).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    loginSuccess.setValue(true);
                } else {
                    errorMessage.setValue("Login Failed. Please check your credentials.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                errorMessage.setValue("Error: " + t.getMessage());
            }
        });
    }

    // Phương thức đăng ký
    public void signUpUser(String email, String username, String password, String fullName) {
        SignUpRequest signUpRequest = new SignUpRequest(email, username, password, fullName);

        apiService.signUp(signUpRequest).enqueue(new Callback<ApiResponse<SignUpResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SignUpResponse>> call, Response<ApiResponse<SignUpResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    signUpSuccess.setValue(true);
                } else {
                    errorMessage.setValue("Sign Up Failed. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SignUpResponse>> call, Throwable t) {
                errorMessage.setValue("Error: " + t.getMessage());
            }
        });
    }
}
