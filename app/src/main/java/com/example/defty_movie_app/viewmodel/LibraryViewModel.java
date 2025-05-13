package com.example.defty_movie_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log; // Import Log for debugging

import com.example.defty_movie_app.data.dto.Category;
import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.PaginationResponse;
import com.example.defty_movie_app.data.model.response.ShowonResponse;
import com.example.defty_movie_app.data.repository.CategoryRepository;
import com.example.defty_movie_app.data.repository.ShowonRepository;
// Removed unused import
// import com.google.android.gms.common.api.Api;

import java.util.ArrayList;
import java.util.List;
// Removed unused import
// import java.util.logging.Filter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryViewModel extends ViewModel {
    private static final String TAG = "LibraryViewModel"; // Tag for logging

    private final ShowonRepository showonRepository;
    private final CategoryRepository categoryRepository;

    public LibraryViewModel() {
        // Get repository instances using Singleton pattern
        showonRepository = ShowonRepository.getInstance();
        categoryRepository = CategoryRepository.getInstance();
    }

    // LiveData for the list of Movies (used in LibraryFragment's RecyclerView)
    private final MutableLiveData<List<Movie>> movieList = new MutableLiveData<>();
    public LiveData<List<Movie>> getMovies() {
        return movieList;
    }

    // LiveData for the list of ShowonResponse (used for sections in HomeFragment)
    private final MutableLiveData<List<ShowonResponse>> showonData = new MutableLiveData<>();
    public LiveData<List<ShowonResponse>> getShowonData() {
        return showonData;
    }

    // LiveData for filter options (Regions, Categories, Release Dates, Paid Categories)
    private final MutableLiveData<List<String>> regions = new MutableLiveData<>();
    // Removed selectedRegion as it's managed in Fragment
    // private final MutableLiveData<String> selectedRegion = new MutableLiveData<>();

    private final MutableLiveData<List<String>> categories = new MutableLiveData<>();
    // Removed selectedCategory as it's managed in Fragment
    // private final MutableLiveData<String> selectedCategory = new MutableLiveData<>();

    private final MutableLiveData<List<Integer>> releaseDate = new MutableLiveData<>();
    // Removed selectedReleaseDate as it's managed in Fragment
    // private final MutableLiveData<String> selectedReleaseDate = new MutableLiveData<>();

    private final MutableLiveData<List<String>> paidCategory = new MutableLiveData<>();
    // Removed selectedPaidCategory as it's managed in Fragment
    // private final MutableLiveData<String> selectedPaidCategory = new MutableLiveData<>();

    // Public getters for filter LiveData
    public MutableLiveData<List<String>> getCategories() {
        return categories;
    }

    public MutableLiveData<List<Integer>> getReleaseDate() {
        return releaseDate;
    }

    public MutableLiveData<List<String>> getRegions() {
        return regions;
    }

    public MutableLiveData<List<String>> getPaidCategory() {
        return paidCategory;
    }

    // Optional: LiveData for loading and error states if needed
    // private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    // public LiveData<Boolean> getLoading() { return isLoading; }
    // private final MutableLiveData<String> error = new MutableLiveData<>();
    // public LiveData<String> getError() { return error; }


    /**
     * Searches/filters movies based on various criteria.
     * Updates the movieList LiveData.
     * @param category The category name (null for all).
     * @param region The region string (null for all).
     * @param releaseYear The release year (null for all).
     * @param paidCategory The paid category integer (1 for Premium, 3 for Normal, null for all).
     */
    public void searchMovies(String category, String region, Integer releaseYear, Integer paidCategory) {
        // isLoading.setValue(true); // Show loading
        Log.d(TAG, "searchMovies called with: category=" + category + ", region=" + region + ", releaseYear=" + releaseYear + ", paidCategory=" + paidCategory);

        categoryRepository.getApi().searchMovie(category, region, releaseYear, paidCategory)
                .enqueue(new Callback<ApiResponse<List<Movie>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Movie>>> call, Response<ApiResponse<List<Movie>>> response) {
                        // isLoading.setValue(false); // Hide loading
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            List<Movie> movies = response.body().getData();
                            Log.d(TAG, "searchMovies onResponse: Received " + movies.size() + " movies.");

                            // Create a new list of Movie DTOs if needed, or directly use the received list
                            // The current code copies data into new Movie objects - ensure Movie DTO is correct
                            List<Movie> movieList1 = new ArrayList<>();
                            for (Movie movieItem : movies) {
                                // Assuming Movie DTO has a constructor or setters for these fields
                                Movie movie = new Movie(
                                        movieItem.getTitle(),
                                        movieItem.getImageUrl(),
                                        movieItem.getSlug(),
                                        movieItem.isPremium() // Assuming isPremium() exists and returns int
                                );
                                movieList1.add(movie);
                            }

                            movieList.setValue(movieList1); // Update movieList LiveData
                        } else {
                            Log.e(TAG, "searchMovies onResponse: API Error " + response.code() + ", Message: " + response.message());
                            movieList.setValue(new ArrayList<>()); // Set empty list on error
                            // error.setValue("API Error: " + response.code() + " - " + response.message()); // Report error
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Movie>>> call, Throwable t) {
                        Log.e(TAG, "searchMovies onFailure: " + t.getMessage(), t);
                        movieList.setValue(new ArrayList<>()); // Set empty list on failure
                        // error.setValue("Network Failure: " + t.getMessage()); // Report error
                        System.out.println("API call failed: " + t.getMessage()); // Keep System.out for quick check
                    }
                });
    }

    /**
     * Fetches filter options (categories, regions, release dates, paid categories).
     * Updates the corresponding LiveData objects.
     */
    public void fetchCategories() {
        // isLoading.setValue(true); // Show loading
        Log.d(TAG, "fetchCategories called.");
        categoryRepository.getApi().getCategories() // Assuming getCategories() fetches all filter options
                .enqueue(new Callback<ApiResponse<Category>>() { // Assuming Category DTO holds all lists
                    @Override
                    public void onResponse(Call<ApiResponse<Category>> call, Response<ApiResponse<Category>> response) {
                        // isLoading.setValue(false); // Hide loading
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Category categoryData = response.body().getData();
                            Log.d(TAG, "fetchCategories onResponse: Received filter data.");

                            // Update filter LiveData objects
                            if (categoryData.getRegions() != null) regions.setValue(categoryData.getRegions());
                            if (categoryData.getReleaseDates() != null) releaseDate.setValue(categoryData.getReleaseDates());
                            if (categoryData.getCategories() != null) categories.setValue(categoryData.getCategories());
                            if (categoryData.getPaidCategories() != null) paidCategory.setValue(categoryData.getPaidCategories());

                        } else {
                            Log.e(TAG, "fetchCategories onResponse: API Error " + response.code() + ", Message: " + response.message());
                            // Set empty lists on error
                            regions.setValue(new ArrayList<>());
                            releaseDate.setValue(new ArrayList<>());
                            categories.setValue(new ArrayList<>());
                            paidCategory.setValue(new ArrayList<>());
                            // error.setValue("API Error fetching filters: " + response.code() + " - " + response.message()); // Report error
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Category>> call, Throwable t) {
                        Log.e(TAG, "fetchCategories onFailure: " + t.getMessage(), t);
                        // Set empty lists on failure
                        regions.setValue(new ArrayList<>());
                        releaseDate.setValue(new ArrayList<>());
                        categories.setValue(new ArrayList<>());
                        paidCategory.setValue(new ArrayList<>());
                        // error.setValue("Network Failure fetching filters: " + t.getMessage()); // Report error
                        System.out.println("API call failed: " + t.getMessage()); // Keep System.out for quick check
                    }
                });
    }

    /**
     * Fetches a list of ShowonResponse objects (sections) based on criteria.
     * Updates the showonData LiveData.
     * @param page The page number.
     * @param size The number of items per page.
     * @param contentType The content type (e.g., "category", null for all).
     * @param contentName The content name (e.g., category name, "" or null for all).
     * @param status The status (e.g., 1).
     */
    public void fetchShowons(int page, int size, String contentType, String contentName, Integer status) {
        // isLoading.setValue(true); // Show loading
        Log.d(TAG, "fetchShowons called with: page=" + page + ", size=" + size + ", contentType=" + contentType + ", contentName=" + contentName + ", status=" + status);

        // --- FIX: Pass the received parameters to the API call ---
        // Use the parameters passed to this method, not hardcoded values
        showonRepository.getApi().getAllShowons(page, size, contentType, contentName, status)
                .enqueue(new Callback<ApiResponse<PaginationResponse<ShowonResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PaginationResponse<ShowonResponse>>> call, Response<ApiResponse<PaginationResponse<ShowonResponse>>> response) {
                        // isLoading.setValue(false); // Hide loading
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            List<ShowonResponse> list = response.body().getData().getContent();
                            Log.d(TAG, "fetchShowons onResponse: Received " + (list != null ? list.size() : "null") + " showons.");
                            showonData.setValue(list); // Update showonData LiveData
                        } else {
                            Log.e(TAG, "fetchShowons onResponse: API Error " + response.code() + ", Message: " + response.message());
                            showonData.setValue(new ArrayList<>()); // Set empty list on error
                            // error.setValue("API Error fetching showons: " + response.code() + " - " + response.message()); // Report error
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PaginationResponse<ShowonResponse>>> call, Throwable t) {
                        Log.e(TAG, "fetchShowons onFailure: " + t.getMessage(), t);
                        showonData.setValue(new ArrayList<>()); // Set empty list on failure
                        // error.setValue("Network Failure fetching showons: " + t.getMessage()); // Report error
                        System.out.println("API call failed: " + t.getMessage()); // Keep System.out for quick check
                    }
                });
    }

    public void fetchMoviesByCategory(int page, int size, String categoryName) {
        showonRepository.getApi().getAllShowons(page, size, "category", categoryName, 1)
                .enqueue(new Callback<ApiResponse<PaginationResponse<ShowonResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PaginationResponse<ShowonResponse>>> call, Response<ApiResponse<PaginationResponse<ShowonResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ShowonResponse> list = response.body().getData().getContent();
                            List<Movie> movies = new ArrayList<>();

                            for (ShowonResponse s : list) {
                                List<Movie> movies1 = s.getContentItems();
                                if (movies1 != null) {
                                    for (Movie movieItem : movies1) {
                                        Movie movie = new Movie(movieItem.getTitle(),
                                                movieItem.getImageUrl(),
                                                movieItem.getSlug(),
                                                movieItem.isPremium());
                                        movies.add(movie);
                                    }
                                }
                            }
                            movieList.setValue(movies);
                        } else {
                            movieList.setValue(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PaginationResponse<ShowonResponse>>> call, Throwable t) {
                        System.out.println("API call failed: " + t.getMessage());
                        movieList.setValue(new ArrayList<>());
                    }
                });
    }
}
