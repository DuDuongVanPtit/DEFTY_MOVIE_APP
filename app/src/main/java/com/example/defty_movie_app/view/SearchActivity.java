package com.example.defty_movie_app.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Import cho FlexboxLayoutManager
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;


import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.HotSearchMovieAdapter;
import com.example.defty_movie_app.adapter.SearchHistoryAdapter;
import com.example.defty_movie_app.data.model.request.MovieNameResponse;
import com.example.defty_movie_app.utils.SearchHistoryManager;
import com.example.defty_movie_app.viewmodel.MovieViewModel;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements SearchHistoryAdapter.OnHistoryItemClickListener {
    private static final String TAG = "SearchActivity";

    private enum SearchState {
        HISTORY_AND_HOT_SEARCH,
        SUGGESTIONS,
        SEARCH_RESULTS
    }
    private SearchState currentSearchState = SearchState.HISTORY_AND_HOT_SEARCH;

    private RecyclerView hotSearchRecyclerView;
    private HotSearchMovieAdapter hotSearchAdapter;
    private MovieViewModel movieViewModel;

    private EditText searchEditText;
    private ImageView backButton;
    private LinearLayout hotSearchContainer;
    private FrameLayout suggestionsFragmentContainer;
    private FrameLayout searchResultFragmentContainer;

    private LinearLayout searchHistoryContainer;
    private RecyclerView searchHistoryRecyclerView;
    private SearchHistoryAdapter searchHistoryAdapter;
    private SearchHistoryManager searchHistoryManager;
    private ImageView clearHistoryButton;


    private SearchSuggestionsFragment suggestionsFragment;
    private SearchResultFragment searchResultFragment;


    public static void start(Context context) {
        Intent intent = new Intent(context, SearchActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        movieViewModel = new ViewModelProvider(this).get(MovieViewModel.class);
        searchHistoryManager = new SearchHistoryManager(this);

        initializeViews();
        setupHotSearchRecyclerView();
        setupSearchHistoryRecyclerView(); // This will now use FlexboxLayoutManager
        setupListeners();
        setupObservers();

        updateUiForState(SearchState.HISTORY_AND_HOT_SEARCH);
        if (hotSearchAdapter.getItemCount() == 0) {
            movieViewModel.fetchHotSearchMovies();
        }
        loadAndDisplaySearchHistory();
    }

    private void initializeViews() {
        hotSearchRecyclerView = findViewById(R.id.hotSearchRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        backButton = findViewById(R.id.backButton);
        hotSearchContainer = findViewById(R.id.hotSearchContainer);
        suggestionsFragmentContainer = findViewById(R.id.suggestionsFragmentContainer);
        searchResultFragmentContainer = findViewById(R.id.searchResultFragmentContainer);

        searchHistoryContainer = findViewById(R.id.searchHistoryContainer);
        searchHistoryRecyclerView = findViewById(R.id.searchHistoryRecyclerView);
        clearHistoryButton = findViewById(R.id.clearHistoryButton);
    }

    private void setupHotSearchRecyclerView() {
        if (hotSearchRecyclerView == null) {
            Log.e(TAG, "hotSearchRecyclerView is null");
            return;
        }
        hotSearchAdapter = new HotSearchMovieAdapter(this);
        hotSearchRecyclerView.setAdapter(hotSearchAdapter);
        hotSearchRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupSearchHistoryRecyclerView() {
        if (searchHistoryRecyclerView == null) {
            Log.e(TAG, "searchHistoryRecyclerView is null");
            return;
        }
        searchHistoryAdapter = new SearchHistoryAdapter(this, this);
        searchHistoryRecyclerView.setAdapter(searchHistoryAdapter);

        // Sử dụng FlexboxLayoutManager để các tag tự xuống dòng
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(this);
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW); // Sắp xếp theo hàng ngang
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP);         // Cho phép xuống dòng
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START); // Căn chỉnh item từ đầu dòng
        searchHistoryRecyclerView.setLayoutManager(flexboxLayoutManager);
    }


    private void setupListeners() {
        backButton.setOnClickListener(v -> handleBackButtonPress());

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (!query.isEmpty()) {
                    if (currentSearchState != SearchState.SUGGESTIONS) {
                        updateUiForState(SearchState.SUGGESTIONS);
                    }
                    movieViewModel.fetchSuggestions(query);
                } else {
                    movieViewModel.clearSuggestions();
                    updateUiForState(SearchState.HISTORY_AND_HOT_SEARCH);
                    loadAndDisplaySearchHistory();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchHistoryManager.addSearchQuery(query);
                    loadAndDisplaySearchHistory();
                    performFullSearch(query);
                }
                return true;
            }
            return false;
        });

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String query = searchEditText.getText().toString().trim();
                if (query.isEmpty()) {
                    updateUiForState(SearchState.HISTORY_AND_HOT_SEARCH);
                    loadAndDisplaySearchHistory();
                } else if (currentSearchState != SearchState.SEARCH_RESULTS && currentSearchState != SearchState.SUGGESTIONS) {
                    // If focused and has text, but not already showing suggestions/results, show suggestions.
                    updateUiForState(SearchState.SUGGESTIONS);
                    movieViewModel.fetchSuggestions(query);
                } else if (currentSearchState == SearchState.SUGGESTIONS) {
                    // If already showing suggestions and gets focus, ensure suggestions are fetched if needed
                    movieViewModel.fetchSuggestions(query);
                }
            }
        });

        clearHistoryButton.setOnClickListener(v -> {
            searchHistoryManager.clearSearchHistory();
            loadAndDisplaySearchHistory();
            Toast.makeText(this, "Đã xóa lịch sử tìm kiếm", Toast.LENGTH_SHORT).show();
        });
    }

    private void performFullSearch(String query) {
        Log.d(TAG, "Performing full search for: " + query);
        hideKeyboard();
        movieViewModel.clearSuggestions();
        updateUiForState(SearchState.SEARCH_RESULTS, query);
    }


    private void updateUiForState(SearchState newState) {
        updateUiForState(newState, null);
    }

    private void updateUiForState(SearchState newState, @Nullable String query) {
        Log.d(TAG, "Updating UI for state: " + newState + (query != null ? " with query: " + query : ""));
        SearchState previousState = currentSearchState;
        currentSearchState = newState;

        // Hide all major containers initially, then show the relevant one(s)
        searchHistoryContainer.setVisibility(View.GONE);
        hotSearchContainer.setVisibility(View.GONE);
        suggestionsFragmentContainer.setVisibility(View.GONE);
        searchResultFragmentContainer.setVisibility(View.GONE);

        // Fragment management
        if (newState != SearchState.SUGGESTIONS && suggestionsFragment != null) {
            removeFragment(suggestionsFragment);
            suggestionsFragment = null;
        }
        if (newState != SearchState.SEARCH_RESULTS && searchResultFragment != null) {
            removeFragment(searchResultFragment);
            searchResultFragment = null;
            if (previousState == SearchState.SEARCH_RESULTS && newState != SearchState.SEARCH_RESULTS) {
                movieViewModel.clearSearchResults(); // Clear data if navigating away from results
            }
        }


        switch (newState) {
            case HISTORY_AND_HOT_SEARCH:
                // Visibility of searchHistoryContainer is handled by loadAndDisplaySearchHistory
                hotSearchContainer.setVisibility(View.VISIBLE);
                loadAndDisplaySearchHistory(); // This will show/hide history container based on content
                break;
            case SUGGESTIONS:
                suggestionsFragmentContainer.setVisibility(View.VISIBLE);
                if (suggestionsFragment == null) {
                    suggestionsFragment = SearchSuggestionsFragment.newInstance();
                }
                replaceFragment(R.id.suggestionsFragmentContainer, suggestionsFragment);
                break;
            case SEARCH_RESULTS:
                searchResultFragmentContainer.setVisibility(View.VISIBLE);
                if (query != null) {
                    searchResultFragment = SearchResultFragment.newInstance(query);
                    replaceFragment(R.id.searchResultFragmentContainer, searchResultFragment);
                }
                break;
        }
    }

    private void replaceFragment(int containerId, Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(containerId, fragment);
        // transaction.addToBackStack(null); // Consider if you want fragment backstack behavior
        transaction.commit();
    }

    private void removeFragment(Fragment fragment) {
        if (fragment != null && fragment.isAdded()) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
    }

    private void loadAndDisplaySearchHistory() {
        List<String> history = searchHistoryManager.getSearchHistory();
        searchHistoryAdapter.updateHistory(history);
        if (history.isEmpty() || currentSearchState != SearchState.HISTORY_AND_HOT_SEARCH) {
            searchHistoryContainer.setVisibility(View.GONE);
        } else {
            searchHistoryContainer.setVisibility(View.VISIBLE);
        }
    }


    private void setupObservers() {
        movieViewModel.getHotSearchMovies().observe(this, moviesList -> {
            if (moviesList != null) {
                Log.d(TAG, "Hot Search Movies LiveData observed. Count: " + moviesList.size());
                hotSearchAdapter.setMovies(moviesList);
            } else {
                hotSearchAdapter.setMovies(new ArrayList<>());
            }
        });
        // Other observers for suggestions and search results would be here or in their respective fragments
    }

    public void onSuggestionClicked(MovieNameResponse movieNameResponse) {
        if (movieNameResponse != null && movieNameResponse.getName() != null) {
            String movieTitle = movieNameResponse.getName();
            searchEditText.setText(movieTitle);
            searchEditText.setSelection(movieTitle.length());
            searchHistoryManager.addSearchQuery(movieTitle);
            // loadAndDisplaySearchHistory(); // performFullSearch will change state, history will be hidden
            performFullSearch(movieTitle);
        }
    }

    @Override
    public void onHistoryItemClick(String query) {
        searchEditText.setText(query);
        searchEditText.setSelection(query.length());
        // searchHistoryManager.addSearchQuery(query); // No need to re-add, it's already in history
        performFullSearch(query);
    }


    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void handleBackButtonPress() {
        String currentQueryInBar = searchEditText.getText().toString().trim();
        switch (currentSearchState) {
            case SEARCH_RESULTS:
                if (!currentQueryInBar.isEmpty() && searchResultFragment != null) {
                    // If there's text in bar while showing results, assume user might want to refine
                    // or see suggestions for that text.
                    updateUiForState(SearchState.SUGGESTIONS);
                    movieViewModel.fetchSuggestions(currentQueryInBar);
                } else {
                    // If bar is empty, or no results fragment, go to initial state
                    searchEditText.setText(""); // Clear text if any
                    updateUiForState(SearchState.HISTORY_AND_HOT_SEARCH);
                }
                break;
            case SUGGESTIONS:
                searchEditText.setText(""); // Clear text
                movieViewModel.clearSuggestions();
                updateUiForState(SearchState.HISTORY_AND_HOT_SEARCH);
                hideKeyboard();
                break;
            case HISTORY_AND_HOT_SEARCH:
            default:
                // super.onBackPressed(); // Call super only if we are actually finishing
                finish(); // Finish activity if at the initial state
                return; // Return to avoid calling super.onBackPressed() again if finish() is called
        }
        // Do not call super.onBackPressed() here if we handled the back press,
        // unless the default behavior is desired for some states not explicitly finishing.
        // In this setup, handleBackButtonPress either transitions state or finishes.
    }


    @Override
    public void onBackPressed() {
        // Let handleBackButtonPress decide if super.onBackPressed() is needed (by calling finish())
        super.onBackPressed();
        handleBackButtonPress();
    }

    public String getCurrentSearchQuery() {
        return searchEditText != null ? searchEditText.getText().toString().trim() : "";
    }
}
