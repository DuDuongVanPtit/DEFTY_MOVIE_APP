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
    private MutableLiveData<List<Movie>> movieList = new MutableLiveData<>();
    private MutableLiveData<List<ShowonResponse>> showonData = new MutableLiveData<>();

    public LibraryViewModel() {
        showonRepository = ShowonRepository.getInstance();
    }

    public LiveData<List<Movie>> getMovies() {
        return movieList;
    }

    public LiveData<List<ShowonResponse>> getShowonData() {
        return showonData;
    }

    // Phương thức này sẽ được gọi khi người dùng chọn một category
    public void fetchMoviesByCategory(int page, int size, String categoryName) {
        System.out.println("Fetching movies for category: " + categoryName);

        showonRepository.getApi().getAllShowons(page, size, "category", categoryName, 1)
                .enqueue(new Callback<ApiResponse<PaginationResponse<ShowonResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PaginationResponse<ShowonResponse>>> call, Response<ApiResponse<PaginationResponse<ShowonResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ShowonResponse> list = response.body().getData().getContent();
                            long total = response.body().getData().getTotalElements();
                            System.out.println("Tổng phần tử: " + total);

                            // Danh sách phim sẽ được lưu ở đây
                            List<Movie> movies = new ArrayList<>();

                            for (ShowonResponse s : list) {
                                System.out.println("Showon: " + s.getContentName());

                                // Lấy contentItems (danh sách phim) từ ShowonResponse
                                List<Movie> movies1 = s.getContentItems();
                                if (movies1 != null) {
                                    // Lặp qua contentItems và tạo Movie object
                                    for (Movie movieItem : movies1) {
                                        Movie movie = new Movie(movieItem.getTitle(), movieItem.getImageUrl(), movieItem.getSlug());
                                        movies.add(movie);
                                        System.out.println(movieItem.getTitle());
                                        System.out.println(movieItem.getImageUrl());
                                        System.out.println(movieItem.getSlug());
                                    }
                                }
                            }

                            // Cập nhật LiveData với danh sách phim đã lấy
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

    // Phương thức để lấy danh sách các category
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
