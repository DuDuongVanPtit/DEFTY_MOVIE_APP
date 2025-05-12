package com.example.defty_movie_app.view;

import android.content.Context;
import android.content.Intent;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.MovieAdapter;
import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.response.ShowonResponse;
import com.example.defty_movie_app.viewmodel.LibraryViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.stream.Collectors;

public class LibraryFragment extends Fragment implements MovieAdapter.OnMovieClickListener {
    private MovieAdapter adapter;
    private LibraryViewModel libraryViewModel;
    private LinearLayout containerLayout;
    private RecyclerView recyclerView;
    private TabLayout tabLayout;
    private ProgressBar progressBar;
    private String currentCategory = null;
    private String selectedRegion = null;
    private String selectedPaidCategory = null;
    private String selectedReleaseDate = null;


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
        setupChipListeners();
        loadInitialData();
    }

    private void initializeViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerView = view.findViewById(R.id.recyclerView);
        containerLayout = view.findViewById(R.id.containerLayout);
    }

    private void addProgressBar() {
        if (getContext() == null) return;
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
        if (getContext() == null) return;
        adapter = new MovieAdapter(getContext(), this);
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
                if (recyclerView.getVisibility() == View.VISIBLE && adapter.getItemCount() == 0) {

                } else if (recyclerView.getVisibility() == View.GONE) {
                    showEmptyView(getString(R.string.no_categories_found));
                }
            }
        });

        libraryViewModel.getMovies().observe(getViewLifecycleOwner(), movies -> {
            hideProgressBar();
            if (movies != null && !movies.isEmpty()) {
                showMovies(movies);
            } else {
                showEmptyView(getString(R.string.no_movies_in_category));
            }
        });

        libraryViewModel.getRegions().observe(getViewLifecycleOwner(), regions -> {
            if (regions != null && !regions.isEmpty()) {
                displayRegions(regions);
            }
        });

        libraryViewModel.getPaidCategory().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && !categories.isEmpty()) {
                displayPaidCategories(categories);
            }
        });

        libraryViewModel.getReleaseDate().observe(getViewLifecycleOwner(), years -> {
            if (years != null && !years.isEmpty()) {
                displayReleaseDate(years);
            }
        });
    }

    private Chip createCustomChip(String text, boolean isChecked) {
        Context context = getContext();
        if (context == null) return null;

        Chip chip = new Chip(context);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(isChecked);
        chip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.chip_background_color));
        chip.setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_color));
        chip.setChipStrokeWidth(0);
        chip.setChipCornerRadius(8f);
        return chip;
    }

    private void setupChipListeners() {
        View view = getView();
        if (view == null) return;

        ChipGroup regionChipGroup = view.findViewById(R.id.regionFilterChips);
        ChipGroup paidCategoryChipGroup = view.findViewById(R.id.paidFilterChips);
        ChipGroup timeChipGroup = view.findViewById(R.id.timeFilterChips);

        regionChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = group.findViewById(checkedIds.get(0));
                selectedRegion = selectedChip != null ? selectedChip.getText().toString() : null;
                applyFilters(currentCategory, selectedRegion, selectedPaidCategory, selectedReleaseDate);
            } else {
                selectedRegion = null;
                applyFilters(currentCategory, selectedRegion, selectedPaidCategory, selectedReleaseDate);
            }
        });

        paidCategoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = group.findViewById(checkedIds.get(0));
                selectedPaidCategory = selectedChip != null ? selectedChip.getText().toString() : null;
                applyFilters(currentCategory, selectedRegion, selectedPaidCategory, selectedReleaseDate);
            } else {
                selectedPaidCategory = null;
                applyFilters(currentCategory, selectedRegion, selectedPaidCategory, selectedReleaseDate);
            }
        });

        timeChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = group.findViewById(checkedIds.get(0));
                selectedReleaseDate = selectedChip != null ? selectedChip.getText().toString() : null;
                applyFilters(currentCategory, selectedRegion, selectedPaidCategory, selectedReleaseDate);
            } else {
                selectedReleaseDate = null;
                applyFilters(currentCategory, selectedRegion, selectedPaidCategory, selectedReleaseDate);
            }
        });
    }

    private void applyFilters(String category, String region, String paidCategory, String releaseDate) {
        showProgressBar();
        Integer adjustedPaidCategory = null;
        if (getString(R.string.all_paid_categories_label).equalsIgnoreCase(paidCategory) || paidCategory == null) {
            adjustedPaidCategory = null;
        } else {
            if (getString(R.string.normal_label).equalsIgnoreCase(paidCategory)) {
                adjustedPaidCategory = 3;
            } else if (getString(R.string.premium_label).equalsIgnoreCase(paidCategory)) {
                adjustedPaidCategory = 1;
            }
        }


        Integer releaseYear = null;
        if (releaseDate != null && !releaseDate.isEmpty() && !getString(R.string.all_time_periods_label).equalsIgnoreCase(releaseDate)) {
            try {
                releaseYear = Integer.parseInt(releaseDate);
            } catch (NumberFormatException e) {
                releaseYear = null;
            }
        } else {
            releaseYear = null;
        }

        String adjustedRegion = region;
        if (getString(R.string.all_regions_label).equalsIgnoreCase(region) || region == null) {
            adjustedRegion = null;
        }

        libraryViewModel.searchMovies(category, adjustedRegion, releaseYear, adjustedPaidCategory);
    }

    private void displayChips(int chipGroupId, String allLabel, List<String> items) {
        View view = getView();
        if (view == null || getContext() == null) return;

        ChipGroup chipGroup = view.findViewById(chipGroupId);
        chipGroup.removeAllViews();

        // Tạo chip "All"
        Chip allChip = createCustomChip(allLabel, true);
        if (allChip != null) {
            chipGroup.addView(allChip);
        }

        if (items != null) {
            for (String item : items) {
                Chip itemChip = createCustomChip(item, false);
                if (itemChip != null) {
                    chipGroup.addView(itemChip);
                }
            }
        }
        if (chipGroup.getCheckedChipId() == View.NO_ID && chipGroup.getChildCount() > 0) {
            Chip firstChip = (Chip) chipGroup.getChildAt(0);
            if (firstChip != null) {
                firstChip.setChecked(true);
            }
        }
    }

    private void displayRegions(List<String> regions) {
        displayChips(R.id.regionFilterChips, getString(R.string.all_regions_label), regions); // Dùng string resource
    }

    private void displayPaidCategories(List<String> paidCategories) {
        displayChips(R.id.paidFilterChips, getString(R.string.all_paid_categories_label), paidCategories);
    }

    private void displayReleaseDate(List<Integer> releaseDates) {
        if (releaseDates == null) return;
        List<String> releaseDateStrings = releaseDates.stream().map(String::valueOf).collect(Collectors.toList());
        displayChips(R.id.timeFilterChips, getString(R.string.all_time_periods_label), releaseDateStrings);
    }


    private void loadInitialData() {
        showProgressBar();
        libraryViewModel.fetchShowons(0, 20, "category", "", 1);
        libraryViewModel.getShowonData().observe(getViewLifecycleOwner(), showons -> {
            if (showons != null && !showons.isEmpty()) {
                String firstCategory = showons.get(0).getContentName();
                if (currentCategory == null) {
                    currentCategory = firstCategory;
                    loadMoviesForCategory(firstCategory);
                    libraryViewModel.fetchCategories();
                }
            } else {
                hideProgressBar();
                if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.no_categories_available), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void setupTabsWithData(TabLayout tabLayout, List<ShowonResponse> showonList) {
        tabLayout.removeAllTabs();
        if (showonList == null) return;

        for (ShowonResponse showon : showonList) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setText(showon.getContentName());
            tab.setTag(showon);
            tabLayout.addTab(tab);
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Object tag = tab.getTag();
                if (tag instanceof ShowonResponse) {
                    ShowonResponse selectedShowon = (ShowonResponse) tag;
                    showProgressBar();
                    currentCategory = selectedShowon.getContentName();
                    loadMoviesForCategory(currentCategory);
                    resetChipSelectionsAndFilters();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Object tag = tab.getTag();
                if (tag instanceof ShowonResponse) {
                    ShowonResponse selectedShowon = (ShowonResponse) tag;
                    showProgressBar();
                    currentCategory = selectedShowon.getContentName();
                    loadMoviesForCategory(currentCategory);
                }
            }
        });
    }

    private void loadMoviesForCategory(String categoryName) {
        libraryViewModel.fetchMoviesByCategory(0, 20, categoryName);
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }

        for (int i = 0; i < containerLayout.getChildCount(); i++) {
            View child = containerLayout.getChildAt(i);
            if (child != progressBar && child != tabLayout && !(child instanceof ChipGroup) ) {

            }
        }
    }


    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showMovies(List<Movie> movies) {
        if (adapter != null) {
            adapter.setMovies(movies);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.VISIBLE);
        }
        TextView emptyTextView = findEmptyView();
        if (emptyTextView != null) {
            emptyTextView.setVisibility(View.GONE);
        }
    }

    private TextView findEmptyView() {
        if (containerLayout == null) return null;
        for (int i = 0; i < containerLayout.getChildCount(); i++) {
            View child = containerLayout.getChildAt(i);
            if (child instanceof TextView && "empty_view".equals(child.getTag())) {
                return (TextView) child;
            }
        }
        return null;
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

    private void resetChipSelectionsAndFilters() {
        View view = getView();
        if (view == null) return;

        ChipGroup regionChipGroup = view.findViewById(R.id.regionFilterChips);
        ChipGroup paidCategoryChipGroup = view.findViewById(R.id.paidFilterChips);
        ChipGroup timeChipGroup = view.findViewById(R.id.timeFilterChips);

        resetChipGroupToFirst(regionChipGroup);
        resetChipGroupToFirst(paidCategoryChipGroup);
        resetChipGroupToFirst(timeChipGroup);
        selectedRegion = getChipTextIfAvailable(regionChipGroup, 0);
        selectedPaidCategory = getChipTextIfAvailable(paidCategoryChipGroup, 0);
        selectedReleaseDate = getChipTextIfAvailable(timeChipGroup, 0);
    }

    private void resetChipGroupToFirst(ChipGroup chipGroup) {
        if (chipGroup != null && chipGroup.getChildCount() > 0) {
            Chip firstChip = (Chip) chipGroup.getChildAt(0);
            if (firstChip != null) {
                firstChip.setChecked(true);
            }
        }
    }

    private String getChipTextIfAvailable(ChipGroup chipGroup, int index) {
        if (chipGroup != null && chipGroup.getChildCount() > index) {
            Chip chip = (Chip) chipGroup.getChildAt(index);
            if (chip != null) {
                return chip.getText().toString();
            }
        }
        return null;
    }

    @Override
    public void onMovieClick(Movie movie) {
        if (getActivity() == null || movie == null) {
            return;
        }
        Intent intent = new Intent(getActivity(), WatchActivity.class);
        String movieSlug = movie.getSlug();

        if (movieSlug != null && !movieSlug.isEmpty()) {
            intent.putExtra("MOVIE_SLUG_ID", movieSlug);
//            Toast.makeText(getContext(), "Mở phim: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), getString(R.string.movie_id_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(intent);
    }
}