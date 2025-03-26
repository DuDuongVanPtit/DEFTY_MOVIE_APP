package com.example.defty_movie_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LoginViewModel extends ViewModel {
    private final MutableLiveData<Boolean> _navigateToPasswordLogin = new MutableLiveData<>();
    public LiveData<Boolean> navigateToPasswordLogin = _navigateToPasswordLogin;

    // Hàm xử lý khi nhấn vào nút "Login with Password"
    public void onLoginWithPasswordClicked() {
        _navigateToPasswordLogin.setValue(true);
    }

    // Reset lại sau khi xử lý xong
    public void resetNavigation() {
        _navigateToPasswordLogin.setValue(false);
    }
}
