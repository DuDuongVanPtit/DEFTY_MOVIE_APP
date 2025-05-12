package com.example.defty_movie_app.viewmodel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.defty_movie_app.data.model.request.LoginRequest;
import com.example.defty_movie_app.data.model.request.SignUpRequest;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.LoginResponse;
import com.example.defty_movie_app.data.model.response.SignUpResponse;
import com.example.defty_movie_app.data.model.response.UserResponse;
import com.example.defty_movie_app.data.remote.AuthApiService;
import com.example.defty_movie_app.data.repository.AuthRepository;
import com.example.defty_movie_app.shared.UserManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends ViewModel {
    private AuthApiService apiService;
    private MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private MutableLiveData<Boolean> signUpSuccess = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<String> tokenLiveData = new MutableLiveData<>();
    private final MutableLiveData<UserResponse> userResponse = new MutableLiveData<>();

    public AuthViewModel() {
        apiService = AuthRepository.getInstance().getApi();
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<Boolean> isSignUpSuccess() {
        return signUpSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    public LiveData<String> getTokenLiveData() {
        return tokenLiveData;
    }
    public LiveData<UserResponse> getUserResponse() {
        return userResponse;
    }
    public void loginUser(String email, String password, Context context) {
        AuthApiService apiService = AuthRepository.getInstance().getApi();
        LoginRequest loginRequest = new LoginRequest(email, password);

        Call<ApiResponse<LoginResponse>> call = apiService.login(loginRequest);
        call.enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LoginResponse>> call, @NonNull Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body().getData();
                    String token = loginResponse.getToken();
                    tokenLiveData.setValue(token);
                    loginSuccess.setValue(true);
                } else {
                    loginSuccess.setValue(false);
                    errorMessage.setValue("Login Failed");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LoginResponse>> call, @NonNull Throwable t) {
                loginSuccess.setValue(false);
                errorMessage.setValue("Error: " + t.getMessage());
            }
        });
    }
    public void signUpUser(String email, String username, String password, String fullName) {
        SignUpRequest signUpRequest = new SignUpRequest(email, username, password, fullName);

        apiService.signUp(signUpRequest).enqueue(new Callback<ApiResponse<SignUpResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SignUpResponse>> call, @NonNull Response<ApiResponse<SignUpResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    signUpSuccess.setValue(true);
                } else {
                    errorMessage.setValue("Sign Up Failed. Please try again.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SignUpResponse>> call, @NonNull Throwable t) {
                errorMessage.setValue("Error: " + t.getMessage());
            }
        });
    }
    public void fetchUserInfo(String token, Context context) {
        apiService.checkAccount(token).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<UserResponse>> call, @NonNull Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse userData = response.body().getData();
                    if (userData != null) {
                        UserManager.saveUser(context, userData.getEmail(), userData.getFullName(), token);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            userResponse.setValue(userData);
                        });
                    } else {
                        System.out.println("User data is null in response");
                    }
                } else {
                    System.out.println("Failed to fetch user info: " +
                            (response.isSuccessful() ? "body is null" : "response not successful"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<UserResponse>> call, @NonNull Throwable t) {
                System.out.println("Error: " + t.getMessage());
            }
        });
    }
}
