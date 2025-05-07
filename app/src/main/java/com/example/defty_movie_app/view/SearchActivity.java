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
import android.widget.ProgressBar; // Vẫn cần nếu hot search có loading riêng
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.HotSearchMovieAdapter;
import com.example.defty_movie_app.data.dto.Movie; // Cần cho onSuggestionClicked
import com.example.defty_movie_app.data.model.request.MovieNameResponse;
import com.example.defty_movie_app.viewmodel.MovieViewModel;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";
    private RecyclerView hotSearchRecyclerView;
    private HotSearchMovieAdapter hotSearchAdapter;
    private MovieViewModel movieViewModel;
    private ProgressBar hotSearchLoadingProgressBar; // Đổi tên cho rõ ràng

    private EditText searchEditText;
    private ImageView backButton; // Nút "X" của bạn
    private LinearLayout hotSearchContainer;
    private FrameLayout suggestionsFragmentContainer;

    private SearchSuggestionsFragment suggestionsFragment;
    private boolean isSuggestionsFragmentVisible = false;


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
        setupHotSearchRecyclerView(); // Đổi tên phương thức
        setupListeners();
        setupObservers();

        // Ban đầu hiển thị hot search
        showHotSearch();
        if (hotSearchAdapter.getItemCount() == 0) { // Chỉ fetch nếu chưa có data
            movieViewModel.fetchHotSearchMovies();
        }
    }

    private void initializeViews() {
        hotSearchRecyclerView = findViewById(R.id.hotSearchRecyclerView);
        // Giả sử bạn có ProgressBar riêng cho hot search trong activity_search.xml
        // hotSearchLoadingProgressBar = findViewById(R.id.hotSearchLoadingProgressBar);

        searchEditText = findViewById(R.id.searchEditText);
        backButton = findViewById(R.id.backButton);
        hotSearchContainer = findViewById(R.id.hotSearchContainer);
        suggestionsFragmentContainer = findViewById(R.id.suggestionsFragmentContainer);
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
        backButton.setOnClickListener(v -> {
            if (isSuggestionsFragmentVisible) {
                searchEditText.setText(""); // Xóa text trong search bar
                hideSuggestions(); // Sẽ tự động gọi showHotSearch
                hideKeyboard();
            } else {
                finish(); // Thoát Activity nếu đang ở màn hình hot search
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (!query.isEmpty()) {
                    showSuggestions();
                    movieViewModel.fetchSuggestions(query);
                } else {
                    hideSuggestions();
                    // Không cần fetchHotSearchMovies ở đây, nó đã được load ban đầu
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    // TODO: Xử lý khi người dùng nhấn nút search trên bàn phím
                    // Ví dụ: Điều hướng đến một màn hình kết quả tìm kiếm đầy đủ
                    Toast.makeText(SearchActivity.this, "Search for: " + query, Toast.LENGTH_SHORT).show();
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });
    }

    private void showHotSearch() {
        hotSearchContainer.setVisibility(View.VISIBLE);
    }

    private void hideHotSearch() {
        hotSearchContainer.setVisibility(View.GONE);
    }

    private void showSuggestions() {
        if (!isSuggestionsFragmentVisible) {
            hideHotSearch();
            suggestionsFragmentContainer.setVisibility(View.VISIBLE);
            if (suggestionsFragment == null) {
                suggestionsFragment = SearchSuggestionsFragment.newInstance();
            }
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.suggestionsFragmentContainer, suggestionsFragment);
            transaction.commit();
            isSuggestionsFragmentVisible = true;
        }
    }

    private void hideSuggestions() {
        if (isSuggestionsFragmentVisible) {
            suggestionsFragmentContainer.setVisibility(View.GONE);
            // Không cần remove fragment ngay, chỉ cần ẩn container
            // Nếu muốn remove:
            // if (suggestionsFragment != null && suggestionsFragment.isAdded()) {
            //     getSupportFragmentManager().beginTransaction().remove(suggestionsFragment).commit();
            // }
            // suggestionsFragment = null; // Reset để lần sau newInstance()
            movieViewModel.clearSuggestions(); // Xóa dữ liệu suggestions cũ
            showHotSearch();
            isSuggestionsFragmentVisible = false;
        }
    }


    private void setupObservers() {
        // Observer cho Hot Search
        movieViewModel.getHotSearchIsLoading().observe(this, isLoading -> {
            // if (hotSearchLoadingProgressBar != null) {
            //    hotSearchLoadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            // }
            // Nếu bạn muốn có ProgressBar riêng cho Hot Search, hãy thêm nó vào layout activity_search.xml
            // và findViewById trong initializeViews()
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

        // Các observer cho suggestions (loading, data, error) đã được xử lý bên trong SearchSuggestionsFragment
        // Tuy nhiên, SearchActivity vẫn có thể cần biết về suggestions data để làm gì đó khác nếu muốn.
    }

    // Được gọi từ SearchSuggestionsFragment khi một suggestion được click
    public void onSuggestionClicked(MovieNameResponse movieNameResponse) { // Tham số là MovieNameResponse
        if (movieNameResponse != null && movieNameResponse.getName() != null) { // Dùng getName()
            searchEditText.setText(movieNameResponse.getName());
            searchEditText.setSelection(searchEditText.getText().length());
            hideKeyboard();
            // Quyết định xem có ẩn suggestions ngay hay không,
            // vì setText sẽ kích hoạt TextWatcher, có thể fetch lại suggestions
            hideSuggestions(); // Cân nhắc hành vi ở đây
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

    @Override
    public void onBackPressed() {
        if (isSuggestionsFragmentVisible) {
            // Nếu suggestions đang hiển thị, hành động back sẽ là xóa text và ẩn suggestions
            searchEditText.setText("");
            hideSuggestions();
            hideKeyboard();
        } else {
            super.onBackPressed(); // Hành động back mặc định (thoát activity)
        }
    }

    public String getCurrentSearchQuery() {
        if (searchEditText != null) {
            return searchEditText.getText().toString().trim();
        }
        return "";
    }
}