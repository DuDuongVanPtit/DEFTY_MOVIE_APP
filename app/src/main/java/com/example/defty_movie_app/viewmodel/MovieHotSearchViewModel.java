package com.example.defty_movie_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.request.MovieNameResponse;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.repository.MovieHotSearchRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieHotSearchViewModel extends ViewModel {

    private final MovieHotSearchRepository movieHotSearchRepository;

    private final MutableLiveData<List<MovieNameResponse>> _searchResults = new MutableLiveData<>();
    public final LiveData<List<MovieNameResponse>> searchResults = _searchResults;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    public MovieHotSearchViewModel() {
        movieHotSearchRepository = MovieHotSearchRepository.getInstance();
    }

    public void fetchHotSearchMovies() {
        _isLoading.setValue(true);
        _error.setValue(null);

        Call<ApiResponse<List<MovieNameResponse>>> call = movieHotSearchRepository.getApi().getBanners();

        call.enqueue(new Callback<ApiResponse<List<MovieNameResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MovieNameResponse>>> call, Response<ApiResponse<List<MovieNameResponse>>> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    _searchResults.setValue(response.body().getData());
                } else {
                    String errorMsg = "API Error: " + response.code() + " " + response.message();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        // Ignore error reading error body
                    }
                    _error.setValue(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MovieNameResponse>>> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue("Network Failure: " + t.getMessage());
            }
        });
    }
}