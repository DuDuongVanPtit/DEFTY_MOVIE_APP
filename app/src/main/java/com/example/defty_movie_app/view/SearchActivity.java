package com.example.defty_movie_app.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.HotSearchMovieAdapter;
import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.request.MovieNameResponse;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.repository.MovieHotSearchRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";
    private EditText searchEditText;
    private ImageView backButton;
    private RecyclerView hotSearchRecyclerView;
    private View hotSearchContainer;
    private View suggestionsFragmentContainer;
    private HotSearchMovieAdapter hotSearchAdapter;
    private MovieHotSearchRepository movieHotSearchRepository;
    private SearchSuggestionsFragment suggestionsFragment;
    private FragmentManager fragmentManager;

    public static void start(Context context) {
        Intent intent = new Intent(context, SearchActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        movieHotSearchRepository = MovieHotSearchRepository.getInstance();
        fragmentManager = getSupportFragmentManager();

        initializeViews();
        setupRecyclerView();
        setupSuggestionsFragment();
        setupClickListeners();
        fetchHotSearchMovies();
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.searchEditText);
        backButton = findViewById(R.id.backButton);
        hotSearchRecyclerView = findViewById(R.id.hotSearchRecyclerView);
        hotSearchContainer = findViewById(R.id.hotSearchContainer);
        suggestionsFragmentContainer = findViewById(R.id.suggestionsFragmentContainer);
    }

    private void setupRecyclerView() {
        if (hotSearchRecyclerView == null) return;
        hotSearchAdapter = new HotSearchMovieAdapter(this);
        hotSearchRecyclerView.setAdapter(hotSearchAdapter);
        hotSearchRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupSuggestionsFragment() {
        suggestionsFragment = SearchSuggestionsFragment.newInstance();
        suggestionsFragment.setOnSuggestionSelectedListener(suggestion -> {
            searchEditText.setText(suggestion);
            fetchMoviesByQuery(suggestion); // Tìm kiếm với gợi ý được chọn
            hideSuggestionsFragment();
        });
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchEditText.getText().toString().trim();
                    if (!query.isEmpty()) {
                        fetchMoviesByQuery(query);
                        hideSuggestionsFragment();
                    } else {
                        Toast.makeText(SearchActivity.this, "Please enter a search query", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });

        // Hiển thị danh sách gợi ý khi nhập văn bản
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (!query.isEmpty()) {
                    fetchSuggestions(query);
                    showSuggestionsFragment();
                } else {
                    hideSuggestionsFragment();
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void fetchHotSearchMovies() {
        Log.d(TAG, "Fetching hot search movies...");
        Call<ApiResponse<List<MovieNameResponse>>> call = movieHotSearchRepository.getApi().getBanners();
        call.enqueue(new Callback<ApiResponse<List<MovieNameResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MovieNameResponse>>> call, Response<ApiResponse<List<MovieNameResponse>>> response) {
                Log.d(TAG, "Hot Search API Response: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<MovieNameResponse> movieNameResponses = response.body().getData();
                    if (movieNameResponses != null) {
                        Log.d(TAG, "Hot Search Movies fetched: " + movieNameResponses.size());
                        List<Movie> movies = new ArrayList<>();
                        for (MovieNameResponse movieNameResponse : movieNameResponses) {
                            movies.add(movieNameResponse.toMovie());
                        }
                        hotSearchAdapter.setMovies(movies);
                    } else {
                        Log.w(TAG, "No data in response");
                        Toast.makeText(SearchActivity.this, "No movies found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "Hot Search API Error: " + response.code() + " " + response.message();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMsg);
                    Toast.makeText(SearchActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MovieNameResponse>>> call, Throwable t) {
                Log.e(TAG, "Hot Search Network Failure: " + t.getMessage());
                Toast.makeText(SearchActivity.this, "Network Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchSuggestions(String query) {
        Log.d(TAG, "Fetching suggestions for query: " + query);
        Call<ApiResponse<List<Movie>>> call = movieHotSearchRepository.getApi().searchMovies(query);
        call.enqueue(new Callback<ApiResponse<List<Movie>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Movie>>> call, Response<ApiResponse<List<Movie>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Movie> movies = response.body().getData();
                    if (movies != null) {
                        List<String> suggestions = new ArrayList<>();
                        for (Movie movie : movies) {
                            suggestions.add(movie.getTitle()); // Giả sử Movie có phương thức getTitle()
                        }
                        suggestionsFragment.setSuggestions(suggestions);
                    }
                } else {
                    Log.e(TAG, "Suggestions API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Movie>>> call, Throwable t) {
                Log.e(TAG, "Suggestions Network Failure: " + t.getMessage());
                Toast.makeText(SearchActivity.this, "Network Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchMoviesByQuery(String query) {
        Log.d(TAG, "Searching movies with query: " + query);
        Call<ApiResponse<List<Movie>>> call = movieHotSearchRepository.getApi().searchMovies(query);
        call.enqueue(new Callback<ApiResponse<List<Movie>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Movie>>> call, Response<ApiResponse<List<Movie>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Movie> movies = response.body().getData();
                    if (movies != null) {
                        hotSearchAdapter.setMovies(movies);
                    }
                } else {
                    Log.e(TAG, "Search API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Movie>>> call, Throwable t) {
                Log.e(TAG, "Search Network Failure: " + t.getMessage());
                Toast.makeText(SearchActivity.this, "Network Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showSuggestionsFragment() {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (!suggestionsFragment.isAdded()) {
            transaction.add(R.id.suggestionsFragmentContainer, suggestionsFragment);
        }
        transaction.show(suggestionsFragment).commit();
        suggestionsFragmentContainer.setVisibility(View.VISIBLE);
        hotSearchContainer.setVisibility(View.GONE);
    }

    private void hideSuggestionsFragment() {
        if (suggestionsFragment.isAdded()) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.hide(suggestionsFragment).commit();
        }
        suggestionsFragmentContainer.setVisibility(View.GONE);
        hotSearchContainer.setVisibility(View.VISIBLE);
    }
}