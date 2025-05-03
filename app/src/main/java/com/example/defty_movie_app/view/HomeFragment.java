package com.example.defty_movie_app.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log; // Import Log for debugging
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.BannerAdapter;
import com.example.defty_movie_app.adapter.MovieHomeAdapter;
import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.response.ShowonResponse;
import com.example.defty_movie_app.viewmodel.BannerViewModel;
import com.example.defty_movie_app.viewmodel.LibraryViewModel;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment"; // Tag for logging

    // --- Views ---
    private TabLayout categoryTabLayout;
    private ViewPager2 bannerViewPager;
    private DotsIndicator dotsIndicator;
    private ProgressBar bannerProgressBar;
    private Button vipButton;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private LinearLayout showonSectionsContainer;
    private NestedScrollView nestedScrollView;
    private ProgressBar showonLoadingProgressBar;


    // --- Adapters ---
    private BannerAdapter bannerAdapter;

    // --- ViewModels ---
    private BannerViewModel bannerViewModel;
    private LibraryViewModel libraryViewModel;

    // --- Drawables ---
    private Drawable toolbarBackgroundDrawable;
    private Drawable defaultToolbarBackgroundDrawable;
    private Drawable categoryBackgroundDefault;
    private Drawable categoryBackgroundGradient;


    // --- Auto Scroll Logic ---
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private Timer autoScrollTimer;
    private final long AUTO_SCROLL_DELAY = 5000;
    private final long AUTO_SCROLL_PERIOD = 5000;
    private volatile boolean isUserDragging = false; // Make volatile for thread visibility

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);
        initializeViews(view);
        loadDrawables();
        // Initialize handler here for safety
        autoScrollHandler = new Handler(Looper.getMainLooper());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bannerViewModel = new ViewModelProvider(this).get(BannerViewModel.class);
        libraryViewModel = new ViewModelProvider(this).get(LibraryViewModel.class);

        setupToolbarScroll();
        setupClickListeners();
        setupTabLayout();
        setupAdaptersAndLayouts();
        observeViewModelData();
        fetchInitialData();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Stopping auto scroll");
        stopAutoScroll(); // Stop scroll when fragment is not visible
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Attempting to restart auto scroll");
        // Restart auto-scroll only if conditions are met
        // Check adapter and view model state thoroughly
        if (!isUserDragging && bannerAdapter != null && bannerAdapter.getItemCount() > 1 &&
                bannerViewModel != null && bannerViewModel.banners.getValue() != null &&
                bannerViewModel.banners.getValue().size() > 1) {
            Log.d(TAG, "onResume: Conditions met, starting auto scroll");
            startAutoScroll();
        } else {
            Log.d(TAG, "onResume: Conditions not met for auto scroll start.");
            if (isUserDragging) Log.d(TAG, "Reason: User is dragging");
            if (bannerAdapter == null) Log.d(TAG, "Reason: bannerAdapter is null");
            else if (bannerAdapter.getItemCount() <= 1) Log.d(TAG, "Reason: bannerAdapter item count <= 1");
            // Add more checks if needed
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: Cleaning up resources");
        stopAutoScroll(); // Ensure timer is cancelled FIRST

        // Remove callbacks and listeners
        if (bannerViewPager != null) {
            bannerViewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
        if (appBarLayout != null) {
            appBarLayout.removeOnOffsetChangedListener(toolbarScrollListener);
        }
        if (categoryTabLayout != null) {
            categoryTabLayout.clearOnTabSelectedListeners();
        }
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable); // Remove pending runnables
        }

        // Nullify views and adapters to help GC and prevent leaks
        bannerViewPager = null;
        dotsIndicator = null;
        bannerProgressBar = null;
        vipButton = null;
        toolbar = null;
        appBarLayout = null;
        showonSectionsContainer = null; // Container is cleared in updateShowonSections anyway
        nestedScrollView = null;
        showonLoadingProgressBar = null;
        bannerAdapter = null; // Nullify adapter
        autoScrollHandler = null;
        autoScrollRunnable = null; // Nullify runnable

        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Cleanup complete");
    }


    private void initializeViews(View view) {
        categoryTabLayout = view.findViewById(R.id.categoryTabLayout);
        vipButton = view.findViewById(R.id.vipButton);
        toolbar = view.findViewById(R.id.toolbar);
        appBarLayout = view.findViewById(R.id.appBarLayout);
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        bannerViewPager = view.findViewById(R.id.bannerViewPager);
        dotsIndicator = view.findViewById(R.id.dotsIndicator);
        bannerProgressBar = view.findViewById(R.id.bannerProgressBar);
        showonSectionsContainer = view.findViewById(R.id.showonSectionsContainer);
        // showonLoadingProgressBar = view.findViewById(R.id.showonLoadingProgressBar); // Uncomment if you have this ID
    }

    private void loadDrawables() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "loadDrawables: Context is null!");
            return;
        }
        toolbarBackgroundDrawable = ContextCompat.getDrawable(context, R.drawable.toolbar_background_gradient);
        defaultToolbarBackgroundDrawable = ContextCompat.getDrawable(context, R.drawable.toolbar_background);
        categoryBackgroundDefault = ContextCompat.getDrawable(context, R.drawable.category_background_default);
        categoryBackgroundGradient = ContextCompat.getDrawable(context, R.drawable.toolbar_background_gradient);
    }

    private final AppBarLayout.OnOffsetChangedListener toolbarScrollListener = (appBarLayout, verticalOffset) -> {
        // Added null check for safety, although less likely here
        if (appBarLayout == null || toolbar == null || categoryTabLayout == null || getContext() == null) return;

        float totalScrollRange = appBarLayout.getTotalScrollRange();
        if (totalScrollRange == 0) return;

        float scrollRatio = (float) Math.abs(verticalOffset) / totalScrollRange;
        float threshold = 0.15f;
        boolean showGradient = scrollRatio >= threshold;

        Drawable targetToolbarBg = showGradient ? toolbarBackgroundDrawable : defaultToolbarBackgroundDrawable;
        if (toolbar.getBackground() != targetToolbarBg) {
            toolbar.setBackground(targetToolbarBg);
        }

        Drawable targetTabBg = showGradient ? categoryBackgroundGradient : categoryBackgroundDefault;
        if (categoryTabLayout.getBackground() != targetTabBg) {
            categoryTabLayout.setBackground(targetTabBg);
        }

        float elevationPx = showGradient && scrollRatio >= 0.95f ?
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()) : 0f;
        if (toolbar.getElevation() != elevationPx) {
            toolbar.setElevation(elevationPx);
        }
    };

    private void setupToolbarScroll() {
        if (toolbar == null || categoryTabLayout == null || appBarLayout == null) return; // Safety check
        toolbar.setBackground(defaultToolbarBackgroundDrawable);
        categoryTabLayout.setBackground(categoryBackgroundDefault);
        toolbar.setElevation(0f);
        appBarLayout.removeOnOffsetChangedListener(toolbarScrollListener);
        appBarLayout.addOnOffsetChangedListener(toolbarScrollListener);
    }


    private void setupClickListeners() {
        Context context = getContext();
        if (context == null || vipButton == null || toolbar == null) return;

        vipButton.setOnClickListener(v -> {
            Toast.makeText(context, "VIP Button Clicked", Toast.LENGTH_SHORT).show();
            // TODO: Handle VIP click action
        });

        View searchBar = toolbar.findViewById(R.id.searchBarLayout);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                Toast.makeText(context, "Search Clicked", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to Search Fragment/Activity
            });
        }
    }

    private void setupTabLayout() {
        Context context = getContext();
        if (context == null || categoryTabLayout == null) return;

        categoryTabLayout.removeAllTabs();
        String[] categories = {"For You", "Drama", "Comedy", "Romance", "Action", "Thriller", "Horror", "Sci-Fi", "Animation"};
        for (String category : categories) {
            categoryTabLayout.addTab(categoryTabLayout.newTab().setText(category));
        }

        if (categoryTabLayout.getTabCount() > 0) {
            TabLayout.Tab firstTab = categoryTabLayout.getTabAt(0);
            if (firstTab != null) {
                firstTab.select();
            }
        }

        categoryTabLayout.clearOnTabSelectedListeners();
        categoryTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String selectedCategory = tab != null && tab.getText() != null ? tab.getText().toString() : "";
                // Check context again inside listener
                if (getContext() != null && !selectedCategory.isEmpty()) {
                    Toast.makeText(getContext(), "Selected: " + selectedCategory, Toast.LENGTH_SHORT).show();
                    // TODO: Implement filtering
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupAdaptersAndLayouts() {
        Context context = getContext();
        // Check essential views
        if (context == null || bannerViewPager == null || dotsIndicator == null) {
            Log.e(TAG, "setupAdaptersAndLayouts: Cannot setup banner - views null or context null");
            return;
        }

        // Initialize adapter only if null
        if (bannerAdapter == null) {
            bannerAdapter = new BannerAdapter(context);
        }
        bannerViewPager.setAdapter(bannerAdapter);

        // --- REMOVED try-catch block for dotsIndicator.detach() ---
        // The detach() method doesn't exist in this library. attachTo() handles re-attachment.

        // Attach the indicator to the ViewPager2
        dotsIndicator.attachTo(bannerViewPager);

        // Register the page change callback here
        bannerViewPager.unregisterOnPageChangeCallback(pageChangeCallback); // Ensure no duplicates
        bannerViewPager.registerOnPageChangeCallback(pageChangeCallback);
    }

    private void observeViewModelData() {
        Context context = getContext();
        if (context == null || getViewLifecycleOwner() == null) {
            Log.e(TAG, "observeViewModelData: Context or LifecycleOwner is null");
            return;
        }

        // --- Banner Observers ---
        bannerViewModel.banners.observe(getViewLifecycleOwner(), banners -> {
            Log.d(TAG, "Banner data observed. Count: " + (banners != null ? banners.size() : "null"));
            stopAutoScroll(); // Stop scroll before updating data

            // Check if views are still valid
            if (bannerAdapter == null || bannerViewPager == null || dotsIndicator == null) {
                Log.w(TAG, "Banner observer: Views became null, cannot update UI.");
                return;
            }

            boolean hasBanners = banners != null && !banners.isEmpty();
            bannerViewPager.setVisibility(hasBanners ? View.VISIBLE : View.GONE);
            dotsIndicator.setVisibility(hasBanners && banners.size() > 1 ? View.VISIBLE : View.GONE);

            if (hasBanners) {
                // IMPORTANT: Pass a *new* list to the adapter to prevent modification issues
                // if the ViewModel reuses the list object.
                bannerAdapter.setBanners(new ArrayList<>(banners)); // Pass a copy
                bannerViewPager.setCurrentItem(0, false); // Reset position without animation

                if (banners.size() > 1 && !isUserDragging) { // Only restart if more than one and user isn't dragging
                    Log.d(TAG, "Banner observer: Starting auto scroll.");
                    startAutoScroll();
                } else {
                    Log.d(TAG, "Banner observer: Not starting auto scroll. Banner count: " + banners.size() + ", isUserDragging: " + isUserDragging);
                }
            } else {
                bannerAdapter.setBanners(new ArrayList<>()); // Clear adapter
                Log.d(TAG, "Banner observer: No banners, clearing adapter.");
            }
        });

        bannerViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && bannerProgressBar != null && bannerViewPager != null && dotsIndicator != null) {
                bannerProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                if (isLoading) {
                    Log.d(TAG, "Banner loading started.");
                    stopAutoScroll();
                    // Hide content while loading for better UX
                    bannerViewPager.setVisibility(View.INVISIBLE);
                    dotsIndicator.setVisibility(View.INVISIBLE);
                } else {
                    Log.d(TAG, "Banner loading finished.");
                    // Visibility will be restored by the banners observer when data arrives
                }
            }
        });

        bannerViewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty() && getContext() != null) {
                Log.e(TAG, "Banner Error: " + error);
                Toast.makeText(getContext(), "Banner Error: " + error, Toast.LENGTH_LONG).show();
                if (bannerViewPager != null) bannerViewPager.setVisibility(View.GONE);
                if (dotsIndicator != null) dotsIndicator.setVisibility(View.GONE);
                if (bannerProgressBar != null) bannerProgressBar.setVisibility(View.GONE);
                stopAutoScroll();
            }
        });

        // --- Showon Observers ---
        libraryViewModel.getShowonData().observe(getViewLifecycleOwner(), showonResponses -> {
            Log.d(TAG, "Showon data observed. Count: " + (showonResponses != null ? showonResponses.size() : "null"));
            Context currentContext = getContext(); // Get context again, might be null later
            if (currentContext == null || showonSectionsContainer == null) {
                Log.w(TAG, "Showon observer: Context or container became null.");
                return;
            }

            // if (showonLoadingProgressBar != null) showonLoadingProgressBar.setVisibility(View.GONE);

            if (showonResponses != null && !showonResponses.isEmpty()) {
                updateShowonSections(showonResponses);
            } else {
                Log.d(TAG, "Showon observer: No sections or null list received.");
                showonSectionsContainer.removeAllViews(); // Clear existing
                // Optional: Display an "empty state" message
                TextView emptyMsg = new TextView(currentContext);
                emptyMsg.setText(showonResponses == null ? "Failed to load movie sections." : "No movie sections available.");
                emptyMsg.setTextColor(ContextCompat.getColor(currentContext, android.R.color.darker_gray));
                emptyMsg.setPadding(0, 40, 0, 40);
                emptyMsg.setGravity(android.view.Gravity.CENTER);
                showonSectionsContainer.addView(emptyMsg);

                if (showonResponses == null) {
                    Toast.makeText(currentContext, "Failed to load movie sections.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Optional: Observe loading/error for showon sections
    }


    private void fetchInitialData() {
        Log.d(TAG, "fetchInitialData: Fetching banners and showons");
        bannerViewModel.fetchBanners(0, 5, null, null);
        // if (showonLoadingProgressBar != null) showonLoadingProgressBar.setVisibility(View.VISIBLE);
        libraryViewModel.fetchShowons(0, 10, null, null, 1);
    }


    private void updateShowonSections(@NonNull List<ShowonResponse> showonList) {
        Context context = getContext();
        if (context == null || showonSectionsContainer == null) {
            Log.w(TAG, "updateShowonSections: Cannot update - Context or container is null.");
            return;
        }
        Log.d(TAG, "updateShowonSections: Updating sections. Count: " + showonList.size());

        showonSectionsContainer.removeAllViews(); // Clear previous sections
        LayoutInflater inflater = LayoutInflater.from(context);

        for (ShowonResponse showonItem : showonList) {
            List<Movie> moviesInSection = showonItem.getContentItems();
            String sectionName = showonItem.getContentName();

            if (sectionName != null && !sectionName.isEmpty() && moviesInSection != null && !moviesInSection.isEmpty()) {
                try {
                    View sectionView = inflater.inflate(R.layout.item_showon_section, showonSectionsContainer, false);

                    TextView sectionTitleTextView = sectionView.findViewById(R.id.sectionTitleTextView);
                    RecyclerView sectionRecyclerView = sectionView.findViewById(R.id.sectionRecyclerView);

                    if (sectionTitleTextView == null || sectionRecyclerView == null) {
                        Log.e(TAG, "updateShowonSections: Missing views in item_showon_section.xml for section: " + sectionName);
                        continue; // Skip this section if views are missing
                    }


                    sectionTitleTextView.setText(sectionName);

                    MovieHomeAdapter sectionAdapter = new MovieHomeAdapter(context); // Use current context
                    sectionRecyclerView.setAdapter(sectionAdapter);
                    sectionRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                    sectionRecyclerView.setHasFixedSize(true);

                    // Pass a *copy* of the movie list to the adapter for safety
                    sectionAdapter.setMovies(new ArrayList<>(moviesInSection));

                    showonSectionsContainer.addView(sectionView);
                } catch (Exception e) {
                    Log.e(TAG, "Error inflating or setting up section: " + sectionName, e);
                    // Optionally show a placeholder or error message for this specific section
                }
            } else {
                Log.w(TAG, "Skipping section due to missing name or empty content: " + (sectionName != null ? sectionName : "[No Name]"));
            }
        }
        Log.d(TAG, "updateShowonSections: Finished updating sections.");
    }


    // --- Banner Auto Scroll Logic ---

    private void startAutoScroll() {
        // Check conditions *before* scheduling
        if (bannerAdapter == null || bannerAdapter.getItemCount() <= 1 || isUserDragging || bannerViewPager == null) {
            Log.d(TAG, "startAutoScroll: Conditions not met, not starting. Adapter count: " + (bannerAdapter != null ? bannerAdapter.getItemCount() : "null") + ", isUserDragging: " + isUserDragging);
            return;
        }

        // Ensure handler is available (should be initialized in onCreateView)
        if (autoScrollHandler == null) {
            Log.e(TAG, "startAutoScroll: Handler is null!");
            return;
        }

        // Create runnable if it doesn't exist
        if (autoScrollRunnable == null) {
            autoScrollRunnable = () -> {
                // --- CRITICAL: Check conditions *inside* the runnable ---
                // Check adapter, viewpager, item count, and dragging state again
                if (bannerAdapter == null || bannerViewPager == null || bannerAdapter.getItemCount() <= 1 || isUserDragging) {
                    Log.d(TAG, "AutoScrollRunnable: Conditions not met, stopping scroll.");
                    stopAutoScroll(); // Stop if conditions change while runnable is pending
                    return;
                }

                try {
                    int currentItem = bannerViewPager.getCurrentItem();
                    // Calculate next item safely
                    int itemCount = bannerAdapter.getItemCount(); // Get count once
                    if (itemCount <= 1) return; // Double check count
                    int nextItem = (currentItem + 1) % itemCount;

                    // Log before scrolling
                    // Log.d(TAG, "AutoScrollRunnable: Scrolling from " + currentItem + " to " + nextItem);

                    bannerViewPager.setCurrentItem(nextItem, true); // Use smooth scroll
                } catch (Exception e) {
                    // Catch potential exceptions during item setting
                    Log.e(TAG, "Exception in autoScrollRunnable", e);
                    stopAutoScroll(); // Stop scrolling if an error occurs
                }
            };
        }

        // Stop any existing timer before starting a new one to prevent duplicates
        stopAutoScroll();

        Log.d(TAG, "startAutoScroll: Scheduling new Timer");
        autoScrollTimer = new Timer();
        try {
            autoScrollTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // Post the scroll action to the main thread handler
                    if (autoScrollHandler != null && autoScrollRunnable != null) {
                        autoScrollHandler.post(autoScrollRunnable);
                    } else {
                        Log.w(TAG, "TimerTask: Handler or Runnable became null, cannot post scroll.");
                        // Attempt to cancel the timer from here if possible/necessary
                        stopAutoScroll();
                    }
                }
            }, AUTO_SCROLL_DELAY, AUTO_SCROLL_PERIOD);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error scheduling autoScrollTimer, possibly already cancelled or invalid state.", e);
            autoScrollTimer = null; // Ensure timer is null if scheduling failed
        }

        // Re-register the callback (done in setupAdaptersAndLayouts and potentially onResume)
        // bannerViewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        // bannerViewPager.registerOnPageChangeCallback(pageChangeCallback);
    }

    private void stopAutoScroll() {
        Log.d(TAG, "stopAutoScroll: Stopping timer and removing callbacks.");
        if (autoScrollTimer != null) {
            try {
                autoScrollTimer.cancel();
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling autoScrollTimer", e);
            }
            autoScrollTimer = null;
        }
        // Remove any pending runnables from the handler
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageScrollStateChanged(int state) {
            // Check adapter existence
            if (bannerAdapter == null) return;

            if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                Log.d(TAG, "PageChangeCallback: User started dragging.");
                isUserDragging = true;
                stopAutoScroll(); // Stop auto-scroll immediately
            } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                // Check if the user *was* dragging before resetting
                if (isUserDragging) {
                    Log.d(TAG, "PageChangeCallback: User stopped dragging.");
                    isUserDragging = false;
                    // Restart auto-scroll only if conditions are still met
                    if (bannerAdapter.getItemCount() > 1) {
                        Log.d(TAG, "PageChangeCallback: Restarting auto scroll after drag.");
                        startAutoScroll();
                    } else {
                        Log.d(TAG, "PageChangeCallback: Not restarting scroll, item count <= 1.");
                    }
                }
            }
        }
        // onPageSelected and onPageScrolled can be overridden if needed for more detailed logging
        @Override
        public void onPageSelected(int position) {
            // Log page selection if helpful for debugging
            // Log.d(TAG, "Page selected: " + position);
        }
    };
}
