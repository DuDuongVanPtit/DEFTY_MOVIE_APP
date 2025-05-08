package com.example.defty_movie_app.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.SearchResultAdapter;
import com.example.defty_movie_app.data.model.response.MovieAppSearchResultResponse;
import com.example.defty_movie_app.viewmodel.MovieViewModel;

import java.util.ArrayList;

public class SearchResultFragment extends Fragment implements SearchResultAdapter.OnMovieClickListener {

    private static final String TAG = "SearchResultFragment";
    private static final String ARG_QUERY = "search_query";

    private MovieViewModel movieViewModel;
    private SearchResultAdapter searchResultAdapter;
    private RecyclerView searchResultRecyclerView;
    private ProgressBar loadingProgressBar;
    private TextView emptyTextView;
    private String currentQuery;

    public static SearchResultFragment newInstance(String query) {
        SearchResultFragment fragment = new SearchResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        fragment.setArguments(args);
        return fragment;
    }

    public SearchResultFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentQuery = getArguments().getString(ARG_QUERY);
        }
        movieViewModel = new ViewModelProvider(requireActivity()).get(MovieViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_result, container, false);
        searchResultRecyclerView = view.findViewById(R.id.searchResultRecyclerView);
        loadingProgressBar = view.findViewById(R.id.searchResultLoadingProgressBar);
        emptyTextView = view.findViewById(R.id.emptySearchResultText);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        searchResultAdapter = new SearchResultAdapter(getContext(), this);
        searchResultRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultRecyclerView.setAdapter(searchResultAdapter);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observeViewModel();
        if (currentQuery != null && !currentQuery.isEmpty()) {
            Log.d(TAG, "Fetching search results for query: " + currentQuery);
            movieViewModel.fetchSearchResults(currentQuery);
        } else {
            Log.w(TAG, "No query provided to SearchResultFragment.");
            emptyTextView.setText("Vui lòng nhập từ khóa tìm kiếm.");
            emptyTextView.setVisibility(View.VISIBLE);
        }
    }

    private void observeViewModel() {
        movieViewModel.getSearchResultsIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                emptyTextView.setVisibility(View.GONE);
                searchResultRecyclerView.setVisibility(View.GONE);
            }
        });

        movieViewModel.getSearchResults().observe(getViewLifecycleOwner(), movies -> {
            Log.d(TAG, "Search results LiveData observed. Count: " + (movies != null ? movies.size() : "null"));
            if (movies != null && !movies.isEmpty()) {
                searchResultAdapter.setMovies(movies);
                emptyTextView.setVisibility(View.GONE);
                searchResultRecyclerView.setVisibility(View.VISIBLE);
            } else if (!movieViewModel.getSearchResultsIsLoading().getValue()){ // Only show empty if not loading
                searchResultAdapter.setMovies(new ArrayList<>()); // Clear adapter
                emptyTextView.setText("Không tìm thấy kết quả nào cho '" + currentQuery + "'.");
                emptyTextView.setVisibility(View.VISIBLE);
                searchResultRecyclerView.setVisibility(View.GONE);
            }
        });

        movieViewModel.getSearchResultsError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Log.e(TAG, "Search Result Error: " + errorMsg);
                Toast.makeText(getContext(), "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                emptyTextView.setText("Đã xảy ra lỗi khi tải kết quả.");
                emptyTextView.setVisibility(View.VISIBLE);
                searchResultRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onMovieClick(MovieAppSearchResultResponse movie) {
        Toast.makeText(getContext(), "Clicked movie: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: Implement navigation to movie details screen
        // Example: MovieDetailActivity.start(getContext(), movie.getSlug());
    }

    @Override
    public void onPlayNowClick(MovieAppSearchResultResponse movie) {
        Toast.makeText(getContext(), "Play now: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: Implement play movie action
    }

    // Call this method from SearchActivity if the query changes while this fragment is visible
    public void performSearch(String newQuery) {
        if (newQuery != null && !newQuery.equals(currentQuery)) {
            currentQuery = newQuery;
            if (getArguments() != null) {
                getArguments().putString(ARG_QUERY, newQuery);
            }
            Log.d(TAG, "New search initiated from activity for query: " + newQuery);
            movieViewModel.clearSearchResults(); // Clear previous results
            searchResultAdapter.setMovies(new ArrayList<>()); // Clear adapter immediately
            movieViewModel.fetchSearchResults(newQuery);
        }
    }
}
