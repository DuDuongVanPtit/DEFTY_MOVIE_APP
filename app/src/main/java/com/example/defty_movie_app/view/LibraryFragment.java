package com.example.defty_movie_app.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.MovieAdapter;
import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.response.ShowonResponse;
import com.example.defty_movie_app.viewmodel.LibraryViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class LibraryFragment extends Fragment {
    private MovieAdapter adapter;
    private LibraryViewModel libraryViewModel;
    private LinearLayout containerLayout;
    private RecyclerView recyclerView;
    private TabLayout tabLayout;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.library_fragment, container, false);
        initializeViews(view);
        addProgressBar();
        setupRecyclerView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        libraryViewModel = new ViewModelProvider(this).get(LibraryViewModel.class);

        setupObservers();

        loadInitialData();
    }

    private void initializeViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerView = view.findViewById(R.id.recyclerView);
        containerLayout = view.findViewById(R.id.containerLayout);
    }

    private void addProgressBar() {
        progressBar = new ProgressBar(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 50, 0, 50);
        progressBar.setLayoutParams(params);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        containerLayout.addView(progressBar);
    }

    private void setupRecyclerView() {
        adapter = new MovieAdapter(getContext());  // Truyền Context cho Adapter
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
    }

    private void setupObservers() {
        libraryViewModel.getShowonData().observe(getViewLifecycleOwner(), showonList -> {
            hideProgressBar();
            if (showonList != null && !showonList.isEmpty()) {
                setupTabsWithData(tabLayout, showonList);
                if (tabLayout.getTabCount() > 0 && tabLayout.getSelectedTabPosition() == -1) {
                    TabLayout.Tab firstTab = tabLayout.getTabAt(0);
                    if (firstTab != null) {
                        firstTab.select();
                    }
                }
            } else {
                showEmptyView("Không có danh mục nào được tìm thấy");
            }
        });

        libraryViewModel.getMovies().observe(getViewLifecycleOwner(), movies -> {
            hideProgressBar();
            if (movies != null && !movies.isEmpty()) {
                showMovies(movies);
            } else {
                showEmptyView("Không có phim nào trong danh mục này");
            }
        });
    }

    private void loadInitialData() {
        showProgressBar();

        libraryViewModel.fetchShowons(0, 20, "category", "", 1);

        libraryViewModel.getShowonData().observe(getViewLifecycleOwner(), showons -> {
            if (showons != null && !showons.isEmpty()) {
                String firstCategory = showons.get(0).getContentName();
                libraryViewModel.fetchMoviesByCategory(0, 20, firstCategory);
            } else {
                hideProgressBar();
                Toast.makeText(getContext(), "Không có danh mục nào", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void setupTabsWithData(TabLayout tabLayout, List<ShowonResponse> showonList) {
        tabLayout.removeAllTabs();
        for (ShowonResponse showon : showonList) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setText(showon.getContentName());
            tab.setTag(showon);
            tabLayout.addTab(tab);
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                ShowonResponse selectedShowon = (ShowonResponse) tab.getTag();
                showProgressBar();
                loadMoviesForCategory(selectedShowon.getContentName());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                ShowonResponse selectedShowon = (ShowonResponse) tab.getTag();
                showProgressBar();
                loadMoviesForCategory(selectedShowon.getContentName());
            }
        });
    }

    private void loadMoviesForCategory(String categoryName) {
        libraryViewModel.fetchMoviesByCategory(0, 20, categoryName);

    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        for (int i = 0; i < containerLayout.getChildCount(); i++) {
            View child = containerLayout.getChildAt(i);
            if (child != progressBar) {
                child.setVisibility(View.GONE);
            }
        }
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    private void showMovies(List<Movie> movies) {
        adapter.setMovies(movies);
        recyclerView.setVisibility(View.VISIBLE);

        for (int i = 0; i < containerLayout.getChildCount(); i++) {
            View child = containerLayout.getChildAt(i);
            if (child != progressBar) {
                child.setVisibility(View.GONE);
            }
        }
    }

    private void showEmptyView(String message) {
        recyclerView.setVisibility(View.GONE);

        TextView emptyTextView = null;
        for (int i = 0; i < containerLayout.getChildCount(); i++) {
            View child = containerLayout.getChildAt(i);
            if (child instanceof TextView && child.getTag() != null && "empty_view".equals(child.getTag())) {
                emptyTextView = (TextView) child;
                break;
            }
        }

        if (emptyTextView == null) {
            emptyTextView = new TextView(getContext());
            emptyTextView.setTag("empty_view");
            emptyTextView.setTextColor(getResources().getColor(android.R.color.white));
            emptyTextView.setTextSize(16);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(16, 50, 16, 50);
            emptyTextView.setLayoutParams(params);
            emptyTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            containerLayout.addView(emptyTextView);
        }

        emptyTextView.setText(message);
        emptyTextView.setVisibility(View.VISIBLE);
    }
}
