package com.example.defty_movie_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.defty_movie_app.data.dto.Category;
import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.PaginationResponse;
import com.example.defty_movie_app.data.model.response.ShowonResponse;
import com.example.defty_movie_app.data.repository.CategoryRepository;
import com.example.defty_movie_app.data.repository.ShowonRepository;
import com.google.android.gms.common.api.Api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Filter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryViewModel extends ViewModel {
    private final ShowonRepository showonRepository;
    private final CategoryRepository categoryRepository;
    public LibraryViewModel() {
        showonRepository = ShowonRepository.getInstance();
        categoryRepository = CategoryRepository.getInstance();
    }

    //Movies
    private final MutableLiveData<List<Movie>> movieList = new MutableLiveData<>();
    public LiveData<List<Movie>> getMovies() {
        return movieList;
    }

    //show on
    private final MutableLiveData<List<ShowonResponse>> showonData = new MutableLiveData<>();
    public LiveData<List<ShowonResponse>> getShowonData() {
        return showonData;
    }

    // Region Filters
    private final MutableLiveData<List<String>> regions = new MutableLiveData<>();

    private final MutableLiveData<String> selectedRegion = new MutableLiveData<>();

    // Category Filters
    private final MutableLiveData<List<String>> categories = new MutableLiveData<>();
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>();

    // Release Date Filters
    private final MutableLiveData<List<Integer>> releaseDate = new MutableLiveData<>();
    private final MutableLiveData<String> selectedReleaseDate = new MutableLiveData<>();

    // Paid Category Filters
    private final MutableLiveData<List<String>> paidCategory = new MutableLiveData<>();
    private final MutableLiveData<String> selectedPaidCategory = new MutableLiveData<>();

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

    public void searchMovies(String category, String region, Integer releaseYear, Integer paidCategory) {
        categoryRepository.getApi().searchMovie(category, region, releaseYear, paidCategory)
                .enqueue(new Callback<ApiResponse<List<Movie>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Movie>>> call, Response<ApiResponse<List<Movie>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Movie> movies = response.body().getData();
                            System.out.println(movies);

                            List<Movie> movieList1 = new ArrayList<>();
                            for (Movie movieItem : movies) {
                                Movie movie = new Movie(
                                        movieItem.getTitle(),
                                        movieItem.getImageUrl(),
                                        movieItem.getSlug(),
                                        movieItem.isPremium()
                                );
                                movieList1.add(movie);
                            }

                            movieList.setValue(movieList1);
                        } else {
                            movieList.setValue(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Movie>>> call, Throwable t) {
                        System.out.println("API call failed: " + t.getMessage());
                        movieList.setValue(new ArrayList<>());
                    }
                });
    }


    public void fetchCategories() {
        categoryRepository.getApi().getCategories()
                .enqueue(new Callback<ApiResponse<Category>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Category>> call, Response<ApiResponse<Category>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<String> listRegions = response.body().getData().getRegions();
                            List<Integer> listReleaseDates = response.body().getData().getReleaseDates();
                            List<String> listCategories = response.body().getData().getCategories();
                            List<String> listPaidCategories = response.body().getData().getPaidCategories();
                            paidCategory.setValue(listPaidCategories);
                            categories.setValue(listCategories);
                            regions.setValue(listRegions);
                            releaseDate.setValue(listReleaseDates);
                        } else {
                            movieList.setValue(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Category>> call, Throwable t) {
                        System.out.println("API call failed: " + t.getMessage());
                        movieList.setValue(new ArrayList<>());
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

    public void fetchShowons(int page, int size, String contentType, String contentName, Integer status) {
        showonRepository.getApi().getAllShowons(page, size, "category", "", 1)
            .enqueue(new Callback<ApiResponse<PaginationResponse<ShowonResponse>>>() {
                @Override
                public void onResponse(Call<ApiResponse<PaginationResponse<ShowonResponse>>> call, Response<ApiResponse<PaginationResponse<ShowonResponse>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<ShowonResponse> list = response.body().getData().getContent();
                        showonData.setValue(list);
                    } else {
                        showonData.setValue(new ArrayList<>());
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<PaginationResponse<ShowonResponse>>> call, Throwable t) {
                    System.out.println("API call failed: " + t.getMessage());
                    showonData.setValue(new ArrayList<>());
                }
            });
    }
}
