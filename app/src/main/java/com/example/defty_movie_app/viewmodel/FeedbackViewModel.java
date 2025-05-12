package com.example.defty_movie_app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.defty_movie_app.data.repository.FeedbackRepository;


public class FeedbackViewModel extends AndroidViewModel {
    private final FeedbackRepository repository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> sendSuccessful = new MutableLiveData<>();

    public FeedbackViewModel(@NonNull Application application) {
        super(application);
        repository = new FeedbackRepository(application);
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getSendSuccessful() {
        return sendSuccessful;
    }

    public void sendFeedback(String message) {
        isLoading.setValue(true);
        repository.sendFeedback(message, new FeedbackRepository.FeedbackCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                sendSuccessful.setValue(true);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }
}