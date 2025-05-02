package com.example.defty_movie_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LanguageViewModel extends ViewModel {
    private final MutableLiveData<String> selectedLanguageCode = new MutableLiveData<>();

    public LiveData<String> getSelectedLanguageCode() {
        return selectedLanguageCode;
    }

    public void setLanguage(String code) {
        selectedLanguageCode.setValue(code);
    }
}
