package com.example.defty_movie_app.viewmodel;

import android.util.Log; // Thêm Log nếu chưa có
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.defty_movie_app.data.model.request.MovieNameResponse; // SỬ DỤNG MovieNameResponse cho cả hai
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.repository.MovieRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieViewModel extends ViewModel {
    private final MovieRepository movieRepository;

    // For Hot Search
    private final MutableLiveData<List<MovieNameResponse>> _hotSearchMovies = new MutableLiveData<>();
    public final LiveData<List<MovieNameResponse>> hotSearchMovies = _hotSearchMovies;
    private final MutableLiveData<Boolean> _hotSearchIsLoading = new MutableLiveData<>();
    public final LiveData<Boolean> hotSearchIsLoading = _hotSearchIsLoading;
    private final MutableLiveData<String> _hotSearchError = new MutableLiveData<>();
    public final LiveData<String> hotSearchError = _hotSearchError;

    // For Search Suggestions - SỬ DỤNG MovieNameResponse
    private final MutableLiveData<List<MovieNameResponse>> _suggestions = new MutableLiveData<>();
    public final LiveData<List<MovieNameResponse>> suggestions = _suggestions;

    private final MutableLiveData<Boolean> _suggestionsIsLoading = new MutableLiveData<>();
    public final LiveData<Boolean> suggestionsIsLoading = _suggestionsIsLoading;

    private final MutableLiveData<String> _suggestionsError = new MutableLiveData<>();
    public final LiveData<String> suggestionsError = _suggestionsError;


    public MovieViewModel() {
        movieRepository = MovieRepository.getInstance();
    }

    public LiveData<List<MovieNameResponse>> getHotSearchMovies() { return hotSearchMovies; }
    public LiveData<Boolean> getHotSearchIsLoading() { return hotSearchIsLoading; }
    public LiveData<String> getHotSearchError() { return hotSearchError; }

    public LiveData<List<MovieNameResponse>> getSuggestions() { return suggestions; } // Kiểu trả về là List<MovieNameResponse>
    public LiveData<Boolean> getSuggestionsLoading() { return suggestionsIsLoading; }
    public LiveData<String> getSuggestionsError() { return suggestionsError; }


    public void fetchHotSearchMovies() {
        _hotSearchIsLoading.setValue(true);
        _hotSearchError.setValue(null);
        Call<ApiResponse<List<MovieNameResponse>>> call = movieRepository.getApi().getMovies();
        call.enqueue(new Callback<ApiResponse<List<MovieNameResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MovieNameResponse>>> call, Response<ApiResponse<List<MovieNameResponse>>> response) {
                _hotSearchIsLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    _hotSearchMovies.setValue(response.body().getData());
                } else {
                    String errorMsg = "API Error (HotSearch): " + response.code() + " " + response.message();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) { /* Bỏ qua */ }
                    _hotSearchError.setValue(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MovieNameResponse>>> call, Throwable t) {
                _hotSearchIsLoading.setValue(false);
                _hotSearchError.setValue("Network Failure (HotSearch): " + t.getMessage());
            }
        });
    }

    public void fetchSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            _suggestions.setValue(new ArrayList<MovieNameResponse>()); // Sử dụng MovieNameResponse
            _suggestionsIsLoading.setValue(false);
            return;
        }
        _suggestionsIsLoading.setValue(true);
        _suggestionsError.setValue(null);
        Log.d("MovieViewModel_Suggest", "Fetching suggestions for query: " + query);

        // Call và Callback sử dụng MovieNameResponse
        Call<ApiResponse<List<MovieNameResponse>>> call = movieRepository.getApi().searchMovies(query);
        call.enqueue(new Callback<ApiResponse<List<MovieNameResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MovieNameResponse>>> call, Response<ApiResponse<List<MovieNameResponse>>> response) {
                _suggestionsIsLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<MovieNameResponse> receivedSuggestions = response.body().getData();
                    Log.d("MovieViewModel_Suggest", "API call successful. Received " + receivedSuggestions.size() + " suggestions.");
                    if (!receivedSuggestions.isEmpty() && receivedSuggestions.get(0) != null) {
                        Log.d("MovieViewModel_Suggest", "First suggestion name from API: '" + receivedSuggestions.get(0).getName() + "'");
                    }
                    _suggestions.setValue(receivedSuggestions);
                } else {
                    String errorMsg = "API Error (Suggestions): " + response.code() + " " + response.message();
                    try {
                        if (response.errorBody() != null) {
                            String errBody = response.errorBody().string();
                            errorMsg += " - " + errBody;
                            Log.e("MovieViewModel_Suggest", "Error body: " + errBody);
                        }
                    } catch (Exception e) { /* Bỏ qua */ }
                    Log.e("MovieViewModel_Suggest", errorMsg);
                    _suggestionsError.setValue(errorMsg);
                    _suggestions.setValue(new ArrayList<MovieNameResponse>()); // Sử dụng MovieNameResponse
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MovieNameResponse>>> call, Throwable t) {
                _suggestionsIsLoading.setValue(false);
                Log.e("MovieViewModel_Suggest", "Network Failure (Suggestions): " + t.getMessage(), t);
                _suggestionsError.setValue("Network Failure (Suggestions): " + t.getMessage());
                _suggestions.setValue(new ArrayList<MovieNameResponse>()); // Sử dụng MovieNameResponse
            }
        });
    }

    public void clearSuggestions() {
        _suggestions.setValue(new ArrayList<MovieNameResponse>()); // Sử dụng MovieNameResponse
        _suggestionsIsLoading.setValue(false);
        _suggestionsError.setValue(null);
    }
}