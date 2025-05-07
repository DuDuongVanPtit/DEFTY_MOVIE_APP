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
import com.example.defty_movie_app.adapter.SearchSuggestionsAdapter;
import com.example.defty_movie_app.data.model.request.MovieNameResponse; // SỬ DỤNG MovieNameResponse
import com.example.defty_movie_app.viewmodel.MovieViewModel;

import java.util.ArrayList;
import java.util.List;

public class SearchSuggestionsFragment extends Fragment implements SearchSuggestionsAdapter.OnSuggestionClickListener {

    private static final String TAG = "SearchSuggestionsFrag";
    private MovieViewModel movieViewModel;
    private SearchSuggestionsAdapter suggestionsAdapter;
    private RecyclerView suggestionsRecyclerView;
    private ProgressBar loadingProgressBar;
    private TextView emptyTextView;

    public static SearchSuggestionsFragment newInstance() {
        return new SearchSuggestionsFragment();
    }

    public SearchSuggestionsFragment() { /* Required empty public constructor */ }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        movieViewModel = new ViewModelProvider(requireActivity()).get(MovieViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_suggestions, container, false);
        suggestionsRecyclerView = view.findViewById(R.id.suggestionsRecyclerView);
        loadingProgressBar = view.findViewById(R.id.suggestionsLoadingProgressBar);
        emptyTextView = view.findViewById(R.id.emptySuggestionsText);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        suggestionsAdapter = new SearchSuggestionsAdapter(getContext(), this);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        suggestionsRecyclerView.setAdapter(suggestionsAdapter);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observeViewModel();
    }

    private void observeViewModel() {
        movieViewModel.getSuggestionsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            if (isLoading) {
                emptyTextView.setVisibility(View.GONE);
                suggestionsRecyclerView.setVisibility(View.GONE);
            }
        });

        movieViewModel.getSuggestions().observe(getViewLifecycleOwner(), (List<MovieNameResponse> movieNameResponses) -> {
            String currentQuery = "";
            if (getActivity() instanceof SearchActivity) {
                currentQuery = ((SearchActivity) getActivity()).getCurrentSearchQuery(); // Lấy query hiện tại
            }

            Log.d(TAG, "Fragment observed suggestions update. Count: " + (movieNameResponses != null ? movieNameResponses.size() : "null list") + ", Query: " + currentQuery);
            if (movieNameResponses != null && !movieNameResponses.isEmpty() && movieNameResponses.get(0) != null) {
                Log.d(TAG, "Fragment received first suggestion name: '" + movieNameResponses.get(0).getName() + "'");
            }

            // TRUYỀN query vào adapter
            suggestionsAdapter.updateSuggestions(movieNameResponses, currentQuery);

            if (movieNameResponses == null || movieNameResponses.isEmpty()) {
                if (loadingProgressBar.getVisibility() == View.GONE) {
                    emptyTextView.setVisibility(View.VISIBLE);
                    suggestionsRecyclerView.setVisibility(View.GONE);
                    Log.d(TAG, "Showing empty text view, hiding RecyclerView.");
                } else {
                    Log.d(TAG, "List is empty/null but loading, so emptyText might not be visible yet.");
                }
            } else {
                emptyTextView.setVisibility(View.GONE);
                suggestionsRecyclerView.setVisibility(View.VISIBLE);
                Log.d(TAG, "Showing RecyclerView with " + movieNameResponses.size() + " items.");
            }
        });

        movieViewModel.getSuggestionsError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "Lỗi tìm kiếm: " + error, Toast.LENGTH_SHORT).show();
                emptyTextView.setText("Lỗi: " + error);
                emptyTextView.setVisibility(View.VISIBLE);
                suggestionsRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onSuggestionClick(MovieNameResponse movieNameResponse) { // Tham số là MovieNameResponse
        if (movieNameResponse != null) {
            Toast.makeText(getContext(), "Clicked suggestion: " + movieNameResponse.getName(), Toast.LENGTH_SHORT).show(); // Dùng getName()
            if (getActivity() instanceof SearchActivity) {
                ((SearchActivity) getActivity()).onSuggestionClicked(movieNameResponse);
            }
        }
    }
}