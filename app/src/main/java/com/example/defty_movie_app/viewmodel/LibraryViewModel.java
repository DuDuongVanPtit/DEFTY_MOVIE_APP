package com.example.defty_movie_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.PaginationResponse;
import com.example.defty_movie_app.data.model.response.ShowonResponse;
import com.example.defty_movie_app.data.repository.ShowonRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryViewModel extends ViewModel {
    private final ShowonRepository showonRepository;
    private final MutableLiveData<List<Movie>> movieList = new MutableLiveData<>();
    private final MutableLiveData<List<ShowonResponse>> showonData = new MutableLiveData<>();
    public LibraryViewModel() {
        showonRepository = ShowonRepository.getInstance();
    }
    public LiveData<List<Movie>> getMovies() {
        return movieList;
    }
    public LiveData<List<ShowonResponse>> getShowonData() {
        return showonData;
    }
    public void fetchMoviesByCategory(int page, int size, String categoryName) {
        System.out.println("Fetching movies for category: " + categoryName);

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
                                                movieItem.getSlug());
                                        movies.add(movie);
                                        System.out.println(movieItem.getTitle());
                                        System.out.println(movieItem.getImageUrl());
                                        System.out.println(movieItem.getSlug());
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
