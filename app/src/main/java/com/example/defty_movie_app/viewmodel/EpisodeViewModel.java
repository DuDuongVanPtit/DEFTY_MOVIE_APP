package com.example.defty_movie_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

// Import necessary classes
import com.example.defty_movie_app.data.model.response.EpisodeResponse; // Import EpisodeResponse
import com.example.defty_movie_app.data.repository.EpisodeRepository; // Import the EpisodeRepository

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EpisodeViewModel extends ViewModel {

    private final EpisodeRepository episodeRepository;

    // LiveData to hold the Episode data
    private final MutableLiveData<EpisodeResponse.Episode> _episode = new MutableLiveData<>();
    public final LiveData<EpisodeResponse.Episode> episode = _episode; // Public immutable LiveData

    // LiveData to track loading state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public final LiveData<Boolean> isLoading = _isLoading;

    // LiveData to report errors
    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    public EpisodeViewModel() {
        // Get repository instance using the Singleton pattern
        episodeRepository = EpisodeRepository.getInstance();
    }

    /**
     * Fetches the first accessible episode for a given movie slug.
     * Updates LiveData objects based on the API call result.
     * Assumes EpisodeUserService.getBanners(String slug) returns Call<EpisodeResponse>
     * where EpisodeResponse contains an Episode object in its 'data' field.
     *
     * @param movieSlug The slug of the movie to fetch the episode for.
     */
    public void fetchFirstEpisode(String movieSlug) {
        _isLoading.setValue(true); // Set loading state to true
        _error.setValue(null);     // Clear previous errors
        _episode.setValue(null);   // Clear previous episode data

        // Get the API call object from the repository
        Call<EpisodeResponse> call = episodeRepository.getApi().getBanners(movieSlug);

        // Execute the call asynchronously
        call.enqueue(new Callback<EpisodeResponse>() {
            @Override
            public void onResponse(Call<EpisodeResponse> call, Response<EpisodeResponse> response) {
                _isLoading.setValue(false); // Set loading state to false
                if (response.isSuccessful() && response.body() != null) {
                    EpisodeResponse episodeResponse = response.body();
                    if (episodeResponse.data != null) {
                        _episode.setValue(episodeResponse.data); // Update episode LiveData with the Episode object
                    } else {
                        // Handle case where response is successful but data is null
                        _error.setValue("Episode data is null in the response.");
                    }
                } else {
                    // Handle API error (non-2xx response)
                    String errorMsg = "API Error: " + response.code() + " " + response.message();
                    try {
                        // Try to get more details from the error body
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        // Ignore error reading error body
                    }
                    _error.setValue(errorMsg); // Update error LiveData
                }
            }

            @Override
            public void onFailure(Call<EpisodeResponse> call, Throwable t) {
                _isLoading.setValue(false); // Set loading state to false
                _error.setValue("Network Failure: " + t.getMessage()); // Update error LiveData
            }
        });
    }

    // Optional: Method to clear the episode data, e.g., when the user navigates away
    public void clearEpisodeData() {
        _episode.setValue(null);
        _isLoading.setValue(false);
        _error.setValue(null);
    }
}
