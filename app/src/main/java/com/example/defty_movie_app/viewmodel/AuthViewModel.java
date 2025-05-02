package com.example.defty_movie_app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends ViewModel {

    private AuthApiService apiService;

    // LiveData để theo dõi kết quả đăng nhập và đăng ký
    private MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private MutableLiveData<Boolean> signUpSuccess = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<UserResponse> userResponse = new MutableLiveData<>();
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
    public LiveData<UserResponse> getUserResponse() {
        return userResponse;
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

    public void fetchUserInfo(String token) {
        apiService.checkAccount(token).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse userData = response.body().getData();
                    if (userData != null) {
                        System.out.println("User data received: " + userData.getFullName());

                        // Thêm debug để xác nhận dữ liệu
                        System.out.println("About to update LiveData with user: " + userData.getFullName());
                        System.out.println("User object is null? " + (userData == null));

                        // Cập nhật LiveData trên main thread
                        new Handler(Looper.getMainLooper()).post(() -> {
                            userResponse.setValue(userData);
                            System.out.println("LiveData updated with user: " + userData.getFullName());
                            System.out.println("Current LiveData value: " + userResponse.getValue().getFullName());
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
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                System.out.println("Error: " + t.getMessage());
            }
        });
    }

}
