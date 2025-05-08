package com.example.defty_movie_app.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.defty_movie_app.data.model.request.MovieNameResponse;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.MovieAppSearchResultResponse; // IMPORT NEW RESPONSE TYPE
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

    // For Search Suggestions
    private final MutableLiveData<List<MovieNameResponse>> _suggestions = new MutableLiveData<>();
    public final LiveData<List<MovieNameResponse>> suggestions = _suggestions;
    private final MutableLiveData<Boolean> _suggestionsIsLoading = new MutableLiveData<>();
    public final LiveData<Boolean> suggestionsIsLoading = _suggestionsIsLoading;
    private final MutableLiveData<String> _suggestionsError = new MutableLiveData<>();
    public final LiveData<String> suggestionsError = _suggestionsError;

    // For Full Search Results
    private final MutableLiveData<List<MovieAppSearchResultResponse>> _searchResults = new MutableLiveData<>();
    public final LiveData<List<MovieAppSearchResultResponse>> searchResults = _searchResults;
    private final MutableLiveData<Boolean> _searchResultsIsLoading = new MutableLiveData<>();
    public final LiveData<Boolean> searchResultsIsLoading = _searchResultsIsLoading;
    private final MutableLiveData<String> _searchResultsError = new MutableLiveData<>();
    public final LiveData<String> searchResultsError = _searchResultsError;


    public MovieViewModel() {
        movieRepository = MovieRepository.getInstance();
    }

    // Getters for Hot Search
    public LiveData<List<MovieNameResponse>> getHotSearchMovies() { return hotSearchMovies; }
    public LiveData<Boolean> getHotSearchIsLoading() { return hotSearchIsLoading; }
    public LiveData<String> getHotSearchError() { return hotSearchError; }

    // Getters for Search Suggestions
    public LiveData<List<MovieNameResponse>> getSuggestions() { return suggestions; }
    public LiveData<Boolean> getSuggestionsLoading() { return suggestionsIsLoading; }
    public LiveData<String> getSuggestionsError() { return suggestionsError; }

    // Getters for Full Search Results
    public LiveData<List<MovieAppSearchResultResponse>> getSearchResults() { return _searchResults; }
    public LiveData<Boolean> getSearchResultsIsLoading() { return _searchResultsIsLoading; }
    public LiveData<String> getSearchResultsError() { return _searchResultsError; }


    public void fetchHotSearchMovies() {
        _hotSearchIsLoading.setValue(true);
        _hotSearchError.setValue(null);
        movieRepository.getApi().getMovies().enqueue(new Callback<ApiResponse<List<MovieNameResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MovieNameResponse>>> call, Response<ApiResponse<List<MovieNameResponse>>> response) {
                _hotSearchIsLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    _hotSearchMovies.setValue(response.body().getData());
                } else {
                    handleApiError(response, _hotSearchError, "HotSearch");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MovieNameResponse>>> call, Throwable t) {
                _hotSearchIsLoading.setValue(false);
                _hotSearchError.setValue("Network Failure (HotSearch): " + t.getMessage());
                Log.e("ViewModel_HotSearch", "Failure: " + t.getMessage(), t);
            }
        });
    }

    public void fetchSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            _suggestions.setValue(new ArrayList<>());
            _suggestionsIsLoading.setValue(false);
            return;
        }
        _suggestionsIsLoading.setValue(true);
        _suggestionsError.setValue(null);
        Log.d("MovieViewModel_Suggest", "Fetching suggestions for query: " + query);

        movieRepository.getApi().searchMovies(query).enqueue(new Callback<ApiResponse<List<MovieNameResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MovieNameResponse>>> call, Response<ApiResponse<List<MovieNameResponse>>> response) {
                _suggestionsIsLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<MovieNameResponse> receivedSuggestions = response.body().getData();
                    Log.d("MovieViewModel_Suggest", "API call successful. Received " + receivedSuggestions.size() + " suggestions.");
                    _suggestions.setValue(receivedSuggestions);
                } else {
                    handleApiError(response, _suggestionsError, "Suggestions");
                    _suggestions.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MovieNameResponse>>> call, Throwable t) {
                _suggestionsIsLoading.setValue(false);
                _suggestionsError.setValue("Network Failure (Suggestions): " + t.getMessage());
                _suggestions.setValue(new ArrayList<>());
                Log.e("ViewModel_Suggest", "Failure: " + t.getMessage(), t);
            }
        });
    }

    public void fetchSearchResults(String query) {
        if (query == null || query.trim().isEmpty()) {
            _searchResults.setValue(new ArrayList<>());
            _searchResultsIsLoading.setValue(false);
            return;
        }
        _searchResultsIsLoading.setValue(true);
        _searchResultsError.setValue(null);
        Log.d("MovieViewModel_Search", "Fetching search results for query: " + query);

        movieRepository.getApi().getMovieSearchResults(query).enqueue(new Callback<ApiResponse<List<MovieAppSearchResultResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MovieAppSearchResultResponse>>> call, Response<ApiResponse<List<MovieAppSearchResultResponse>>> response) {
                _searchResultsIsLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<MovieAppSearchResultResponse> results = response.body().getData();
                    Log.d("MovieViewModel_Search", "API call successful. Received " + results.size() + " results.");
                    _searchResults.setValue(results);
                } else {
                    handleApiError(response, _searchResultsError, "SearchResults");
                    _searchResults.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MovieAppSearchResultResponse>>> call, Throwable t) {
                _searchResultsIsLoading.setValue(false);
                _searchResultsError.setValue("Network Failure (SearchResults): " + t.getMessage());
                _searchResults.setValue(new ArrayList<>());
                Log.e("ViewModel_Search", "Failure: " + t.getMessage(), t);
            }
        });
    }


    public void clearSuggestions() {
        _suggestions.setValue(new ArrayList<>());
        _suggestionsIsLoading.setValue(false);
        _suggestionsError.setValue(null);
    }

    public void clearSearchResults() {
        _searchResults.setValue(new ArrayList<>());
        _searchResultsIsLoading.setValue(false);
        _searchResultsError.setValue(null);
    }

    private <T> void handleApiError(Response<ApiResponse<T>> response, MutableLiveData<String> errorLiveData, String apiName) {
        String errorMsg = "API Error (" + apiName + "): " + response.code() + " " + response.message();
        try {
            if (response.errorBody() != null) {
                String errBody = response.errorBody().string();
                errorMsg += " - " + errBody;
                Log.e("MovieViewModel_" + apiName, "Error body: " + errBody);
            }
        } catch (Exception e) {
            Log.e("MovieViewModel_" + apiName, "Error parsing error body", e);
        }
        errorLiveData.setValue(errorMsg);
        Log.e("MovieViewModel_" + apiName, errorMsg);
    }
}
