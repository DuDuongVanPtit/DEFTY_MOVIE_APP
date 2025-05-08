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
// import android.widget.ProgressBar; // ProgressBar is now inside fragments
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.HotSearchMovieAdapter;
import com.example.defty_movie_app.data.model.request.MovieNameResponse;
import com.example.defty_movie_app.viewmodel.MovieViewModel;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";

    private enum SearchState {
        HOT_SEARCH,
        SUGGESTIONS,
        SEARCH_RESULTS
    }
    private SearchState currentSearchState = SearchState.HOT_SEARCH;

    private RecyclerView hotSearchRecyclerView;
    private HotSearchMovieAdapter hotSearchAdapter;
    private MovieViewModel movieViewModel;

    private EditText searchEditText;
    private ImageView backButton;
    private LinearLayout hotSearchContainer;
    private FrameLayout suggestionsFragmentContainer;
    private FrameLayout searchResultFragmentContainer; // New container

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
        initializeViews();
        setupHotSearchRecyclerView();
        setupListeners();
        setupObservers();

        // Initially show hot search
        updateUiForState(SearchState.HOT_SEARCH);
        if (hotSearchAdapter.getItemCount() == 0) {
            movieViewModel.fetchHotSearchMovies();
        }
    }

    private void initializeViews() {
        hotSearchRecyclerView = findViewById(R.id.hotSearchRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        backButton = findViewById(R.id.backButton);
        hotSearchContainer = findViewById(R.id.hotSearchContainer);
        suggestionsFragmentContainer = findViewById(R.id.suggestionsFragmentContainer);
        searchResultFragmentContainer = findViewById(R.id.searchResultFragmentContainer); // Initialize new container
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
                    // If query is empty, go back to hot search
                    movieViewModel.clearSuggestions();
                    updateUiForState(SearchState.HOT_SEARCH);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    performFullSearch(query);
                }
                return true;
            }
            return false;
        });

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !searchEditText.getText().toString().trim().isEmpty() && currentSearchState != SearchState.SEARCH_RESULTS) {
                updateUiForState(SearchState.SUGGESTIONS);
                movieViewModel.fetchSuggestions(searchEditText.getText().toString().trim());
            }
        });
    }

    private void performFullSearch(String query) {
        Log.d(TAG, "Performing full search for: " + query);
        hideKeyboard();
        movieViewModel.clearSuggestions(); // Clear any existing suggestions
        updateUiForState(SearchState.SEARCH_RESULTS, query);
        // ViewModel will be triggered by the fragment itself using this query
    }


    private void updateUiForState(SearchState newState) {
        updateUiForState(newState, null);
    }

    private void updateUiForState(SearchState newState, @Nullable String query) {
        currentSearchState = newState;

        hotSearchContainer.setVisibility(View.GONE);
        suggestionsFragmentContainer.setVisibility(View.GONE);
        searchResultFragmentContainer.setVisibility(View.GONE);
        removeFragment(suggestionsFragment);
        suggestionsFragment = null;
        // We don't remove searchResultFragment immediately to preserve its state if user clicks back then search again.
        // It will be replaced if a new search is made.

        switch (newState) {
            case HOT_SEARCH:
                hotSearchContainer.setVisibility(View.VISIBLE);
                movieViewModel.clearSearchResults(); // Clear previous search results
                removeFragment(searchResultFragment); // Remove search result fragment
                searchResultFragment = null;
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
                    if (searchResultFragment != null && searchResultFragment.isAdded() && !searchResultFragment.isVisible()) {
                        // If fragment exists but is hidden, just make it visible and update its query if needed
                        // This case might be complex if query changes, simpler to replace or ensure fragment handles new query
                        getSupportFragmentManager().beginTransaction().show(searchResultFragment).commit();
                        searchResultFragment.performSearch(query); // Tell fragment to search new query
                    } else {
                        searchResultFragment = SearchResultFragment.newInstance(query);
                        replaceFragment(R.id.searchResultFragmentContainer, searchResultFragment);
                    }
                }
                break;
        }
    }

    private void replaceFragment(int containerId, Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(containerId, fragment);
        transaction.commit();
    }

    private void removeFragment(Fragment fragment) {
        if (fragment != null && fragment.isAdded()) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
    }


    private void setupObservers() {
        movieViewModel.getHotSearchIsLoading().observe(this, isLoading -> {
            // Loading indicator for hot search is not explicitly defined in activity_search.xml
            // If needed, add a ProgressBar to hotSearchContainer
            Log.d(TAG, "Hot Search Loading: " + isLoading);
        });

        movieViewModel.getHotSearchMovies().observe(this, moviesList -> {
            if (moviesList != null) {
                Log.d(TAG, "Hot Search Movies LiveData observed. Count: " + moviesList.size());
                hotSearchAdapter.setMovies(moviesList);
            } else {
                hotSearchAdapter.setMovies(new ArrayList<>());
            }
        });

        movieViewModel.getHotSearchError().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Log.e(TAG, "Hot Search Error: " + errorMsg);
                Toast.makeText(SearchActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Called from SearchSuggestionsFragment when a suggestion is clicked
    public void onSuggestionClicked(MovieNameResponse movieNameResponse) {
        if (movieNameResponse != null && movieNameResponse.getName() != null) {
            String movieTitle = movieNameResponse.getName();
            searchEditText.setText(movieTitle);
            searchEditText.setSelection(movieTitle.length()); // Move cursor to end
            performFullSearch(movieTitle); // Directly perform search on suggestion click
        }
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
        switch (currentSearchState) {
            case SEARCH_RESULTS:
                // If showing results, and search bar has text, go to suggestions.
                // If search bar is empty (e.g., user cleared it), go to hot search.
                String currentQuery = searchEditText.getText().toString().trim();
                if (!currentQuery.isEmpty()) {
                    updateUiForState(SearchState.SUGGESTIONS);
                    movieViewModel.fetchSuggestions(currentQuery); // Re-fetch suggestions for current text
                } else {
                    updateUiForState(SearchState.HOT_SEARCH);
                }
                break;
            case SUGGESTIONS:
                // If showing suggestions, clear text and go to hot search
                searchEditText.setText("");
                movieViewModel.clearSuggestions();
                updateUiForState(SearchState.HOT_SEARCH);
                hideKeyboard();
                break;
            case HOT_SEARCH:
            default:
                finish(); // Default behavior: exit activity
                break;
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handleBackButtonPress();
        // super.onBackPressed() is called conditionally within handleBackButtonPress or if it falls through
    }

    public String getCurrentSearchQuery() {
        return searchEditText != null ? searchEditText.getText().toString().trim() : "";
    }
}
