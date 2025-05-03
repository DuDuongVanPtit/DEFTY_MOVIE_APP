package com.example.defty_movie_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

// Import necessary classes
import com.example.defty_movie_app.data.dto.Banner; // Import Banner DTO
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.PaginationResponse;
import com.example.defty_movie_app.data.repository.BannerRepository;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BannerViewModel extends ViewModel {

    private final BannerRepository bannerRepository;

    // LiveData to hold the list of banners (using Banner type)
    private final MutableLiveData<List<Banner>> _banners = new MutableLiveData<>();
    public final LiveData<List<Banner>> banners = _banners; // Public immutable LiveData

    // LiveData to track loading state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public final LiveData<Boolean> isLoading = _isLoading;

    // LiveData to report errors
    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    public BannerViewModel() {
        // Get repository instance (Consider using Dependency Injection)
        bannerRepository = BannerRepository.getInstance();
    }

    /**
     * Fetches banners from the repository based on query parameters.
     * Updates LiveData objects based on the API call result.
     * IMPORTANT: Assumes BannerApiService now returns Call<ApiResponse<PaginationResponse<Banner>>>
     *
     * @param page   The page number to fetch.
     * @param size   The number of items per page.
     * @param title  Optional title filter.
     * @param status Optional status filter.
     */
    public void fetchBanners(int page, int size, String title, Integer status) {
        _isLoading.setValue(true); // Set loading state to true
        _error.setValue(null);     // Clear previous errors

        // Get the API call object - IMPORTANT: Assumes getBanners now returns Call<...<Banner>>>
        // If BannerApiService still returns ShowonResponse, this needs conversion logic
        Call<ApiResponse<PaginationResponse<Banner>>> call = bannerRepository.getApi().getBanners(page, size, title, status);

        // Execute the call asynchronously
        call.enqueue(new Callback<ApiResponse<PaginationResponse<Banner>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaginationResponse<Banner>>> call, Response<ApiResponse<PaginationResponse<Banner>>> response) {
                _isLoading.setValue(false); // Set loading state to false
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    // Assuming the structure: ApiResponse -> getData() -> PaginationResponse -> getContent()
                    // Now expecting a List<Banner>
                    List<Banner> bannerList = response.body().getData().getContent();
                    if (bannerList != null) {
                        _banners.setValue(bannerList); // Update banner list LiveData with List<Banner>
                    } else {
                        _error.setValue("Banner list is null in the response data.");
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
            public void onFailure(Call<ApiResponse<PaginationResponse<Banner>>> call, Throwable t) {
                _isLoading.setValue(false); // Set loading state to false
                _error.setValue("Network Failure: " + t.getMessage()); // Update error LiveData
            }
        });
    }
}
