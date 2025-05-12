package com.example.defty_movie_app.view;

import android.content.Intent; // <-- THÊM IMPORT NÀY
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
// import com.example.defty_movie_app.view.WatchActivity; // WatchActivity cùng package nên không cần import tường minh

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
            } else if (Boolean.FALSE.equals(movieViewModel.getSearchResultsIsLoading().getValue())){ // Only show empty if not loading
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
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot start WatchActivity.");
            return;
        }
        if (movie != null && movie.getSlug() != null) {
            Toast.makeText(getContext(), "Đang mở phim: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), WatchActivity.class);
            intent.putExtra("MOVIE_SLUG_ID", movie.getSlug()); // Sử dụng key "MOVIE_SLUG" như trong MovieHomeAdapter
            getContext().startActivity(intent);
        } else {
            Log.e(TAG, "Movie data or slug is null. Cannot navigate to WatchActivity.");
            Toast.makeText(getContext(), "Không thể mở phim, thiếu dữ liệu.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPlayNowClick(MovieAppSearchResultResponse movie) {
        // Tương tự, nếu nút "Play now" cũng cần điều hướng đến WatchActivity
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot start WatchActivity from PlayNowClick.");
            return;
        }
        if (movie != null && movie.getSlug() != null) {
            Toast.makeText(getContext(), "Đang phát ngay: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), WatchActivity.class);
            intent.putExtra("MOVIE_SLUG", movie.getSlug());
            getContext().startActivity(intent);
        } else {
            Log.e(TAG, "Movie data or slug is null. Cannot play now.");
            Toast.makeText(getContext(), "Không thể phát phim, thiếu dữ liệu.", Toast.LENGTH_SHORT).show();
        }
    }

}