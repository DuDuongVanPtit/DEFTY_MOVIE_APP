package com.example.defty_movie_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.defty_movie_app.model.ApiResponse;
import com.example.defty_movie_app.model.LoginRequest;
import com.example.defty_movie_app.model.LoginResponse;
import com.example.defty_movie_app.model.RegisterRequest;
import com.example.defty_movie_app.model.UserResponse;
import com.example.defty_movie_app.repository.AuthRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends ViewModel {
    private AuthRepository repository;
    private MutableLiveData<ApiResponse<LoginResponse>> loginResult = new MutableLiveData<>();
    private MutableLiveData<ApiResponse<UserResponse>> registerResult = new MutableLiveData<>();

    public AuthViewModel() {
        repository = new AuthRepository();
    }

    public LiveData<ApiResponse<LoginResponse>> getLoginResult() {
        return loginResult;
    }

    public LiveData<ApiResponse<UserResponse>> getRegisterResult() {
        return registerResult;
    }

    public void login(String username, String password) {
        repository.getApi().login(new LoginRequest(username, password)).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                loginResult.setValue(response.body());
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                loginResult.setValue(null);
            }
        });
    }

    public void register(String username, String password, String email) {
        repository.getApi().register(new RegisterRequest(username, password, email)).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                registerResult.setValue(response.body());
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                registerResult.setValue(null);
            }
        });
    }
}
