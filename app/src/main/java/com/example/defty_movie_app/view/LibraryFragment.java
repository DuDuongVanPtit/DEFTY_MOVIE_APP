package com.example.defty_movie_app.view;

import android.content.Context;
import android.content.res.ColorStateList;
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
import java.util.Objects;
import java.util.stream.Collectors;

public class LibraryFragment extends Fragment {
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
        Chip chip = new Chip(context);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(isChecked);
        assert context != null;
        chip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.chip_background_color));
        chip.setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_color));
        chip.setChipStrokeWidth(0);
        chip.setChipStrokeColor(null);
        chip.setChipCornerRadius(8f);
        return chip;
    }

    private void setupChipListeners() {
        ChipGroup regionChipGroup = requireView().findViewById(R.id.regionFilterChips);
        ChipGroup paidCategoryChipGroup = requireView().findViewById(R.id.paidFilterChips);
        ChipGroup timeChipGroup = requireView().findViewById(R.id.timeFilterChips);

        regionChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selectedChip = group.findViewById(checkedId);
            selectedRegion = selectedChip != null ? selectedChip.getText().toString() : null;
            applyFilters(currentCategory, selectedRegion, selectedPaidCategory, selectedReleaseDate);
        });

        paidCategoryChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selectedChip = group.findViewById(checkedId);
            selectedPaidCategory = selectedChip != null ? selectedChip.getText().toString() : null;
            applyFilters(currentCategory, selectedRegion, selectedPaidCategory, selectedReleaseDate);
        });

        timeChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selectedChip = group.findViewById(checkedId);
            selectedReleaseDate = selectedChip != null ? selectedChip.getText().toString() : null;
            applyFilters(currentCategory, selectedRegion, selectedPaidCategory, selectedReleaseDate);
        });
    }

    private void applyFilters(String category, String region, String paidCategory, String releaseDate) {
        showProgressBar();
        Integer adjustedPaidCategory = null;
        if ("All Paid Categories".equalsIgnoreCase(paidCategory)) {
            adjustedPaidCategory = null;
        } else if (paidCategory != null) {
            // Xử lý trường hợp còn lại
            if ("normal".equalsIgnoreCase(paidCategory)) {
                adjustedPaidCategory = 3;
            } else {
                adjustedPaidCategory = 1;
            }
        }

        Integer releaseYear = null;
        if (releaseDate != null && !releaseDate.isEmpty()) {
            try {
                releaseYear = Integer.parseInt(releaseDate);
            } catch (NumberFormatException e) {
                releaseYear = null;
            }
        } else if ("All Time Periods".equalsIgnoreCase(releaseDate)) {
            releaseYear = null;
        }

        if ("All Regions".equalsIgnoreCase(region)) {
            region = null;
        }

        libraryViewModel.searchMovies(category, region, releaseYear, adjustedPaidCategory);
    }



    private void displayChips(int chipGroupId, String allLabel, List<String> items) {
        ChipGroup chipGroup = requireView().findViewById(chipGroupId);
        chipGroup.removeAllViews();
        chipGroup.addView(createCustomChip(allLabel, true));
        for (String item : items) {
            chipGroup.addView(createCustomChip(item, false));
        }
    }

    private void displayRegions(List<String> regions) {
        displayChips(R.id.regionFilterChips, "All Regions", regions);
    }

    private void displayPaidCategories(List<String> paidCategories) {
        displayChips(R.id.paidFilterChips, "All Paid Categories", paidCategories);
    }

    private void displayReleaseDate(List<Integer> releaseDates) {
        List<String> releaseDateStrings = releaseDates.stream().map(String::valueOf).collect(Collectors.toList());
        displayChips(R.id.timeFilterChips, "All Time Periods", releaseDateStrings);
    }



    private void loadInitialData() {
        showProgressBar();
        libraryViewModel.fetchShowons(0, 20, "category", "", 1);
        libraryViewModel.fetchCategories();
        libraryViewModel.getShowonData().observe(getViewLifecycleOwner(), showons -> {
            if (showons != null && !showons.isEmpty()) {
                String firstCategory = showons.get(0).getContentName();
                libraryViewModel.fetchMoviesByCategory(0, 20, firstCategory);
                currentCategory = firstCategory;
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
                currentCategory = selectedShowon.getContentName();
                resetChipSelections();
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
        currentCategory = categoryName;
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
    private void resetChipSelections() {
        ChipGroup regionChipGroup = requireView().findViewById(R.id.regionFilterChips);
        ChipGroup paidCategoryChipGroup = requireView().findViewById(R.id.paidFilterChips);
        ChipGroup timeChipGroup = requireView().findViewById(R.id.timeFilterChips);

        // Reset filter vùng về "All Regions"
        for (int i = 0; i < regionChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) regionChipGroup.getChildAt(i);
            chip.setChecked("All Regions".equals(chip.getText().toString()));
        }

        // Reset filter loại thanh toán về "All Paid Categories"
        for (int i = 0; i < paidCategoryChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) paidCategoryChipGroup.getChildAt(i);
            chip.setChecked("All Paid Categories".equals(chip.getText().toString()));
        }

        // Reset filter thời gian về "All Time Periods"
        for (int i = 0; i < timeChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) timeChipGroup.getChildAt(i);
            chip.setChecked("All Time Periods".equals(chip.getText().toString()));
        }

        // Đặt lại các biến lựa chọn region, paidCategory và releaseDate
        selectedRegion = "All Regions";
        selectedPaidCategory = "All Paid Categories";
        selectedReleaseDate = "All Time Periods";

        // Áp dụng lại các bộ lọc
        applyFilters(currentCategory, selectedRegion, selectedPaidCategory, selectedReleaseDate);
    }
}
