package com.example.defty_movie_app.view;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.BannerAdapter;
import com.example.defty_movie_app.adapter.MovieHomeAdapter;
import com.example.defty_movie_app.viewmodel.BannerViewModel;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment {

    // --- Views ---
    private TabLayout categoryTabLayout;
    private ViewPager2 bannerViewPager;
    private DotsIndicator dotsIndicator;
    private ProgressBar bannerProgressBar;
    private Button vipButton;
    private Toolbar toolbar;
    private View headerLayout;
    private AppBarLayout appBarLayout;

    // Movie Sections Views
    private RecyclerView limitedFreeRecyclerView;
    private RecyclerView topShowsRecyclerView;
    private RecyclerView premiumContentRecyclerView;
    private TextView limitedFreeTitleTextView;
    private TextView topShowsTitleTextView;
    private TextView premiumContentTitleTextView;

    // --- Adapters ---
    private BannerAdapter bannerAdapter;
    private MovieHomeAdapter limitedFreeAdapter;
    private MovieHomeAdapter topShowsAdapter;
    private MovieHomeAdapter premiumContentAdapter;

    // --- ViewModels ---
    private BannerViewModel bannerViewModel;

    // --- Drawables for toolbar background ---
    private Drawable toolbarBackgroundDrawable;
    private Drawable defaultToolbarBackgroundDrawable;

    // --- Auto Scroll Logic ---
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private Timer autoScrollTimer;
    private final long AUTO_SCROLL_DELAY = 5000;
    private final long AUTO_SCROLL_PERIOD = 5000;
    private boolean isUserDragging = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);
        initializeViews(view);
        loadDrawables(); // Tải các drawable cần thiết
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bannerViewModel = new ViewModelProvider(this).get(BannerViewModel.class);
        setupToolbarScroll();
        setupClickListeners();
        setupTabLayout();
        setupAdaptersAndLayouts();
        observeViewModelData();
        fetchInitialBanners();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bannerViewModel != null && bannerViewModel.banners.getValue() != null && bannerViewModel.banners.getValue().size() > 1) {
            startAutoScroll();
        }
    }

    @Override
    public void onDestroyView() {
        stopAutoScroll();
        autoScrollHandler = null;
        autoScrollRunnable = null;
        if (bannerViewPager != null) {
            bannerViewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
        super.onDestroyView();
    }

    private void initializeViews(View view) {
        // Header & Tabs
        categoryTabLayout = view.findViewById(R.id.categoryTabLayout);
        vipButton = view.findViewById(R.id.vipButton);
        toolbar = view.findViewById(R.id.toolbar);
        headerLayout = view.findViewById(R.id.headerLayout);
        appBarLayout = view.findViewById(R.id.appBarLayout);

        // Banner Section
        bannerViewPager = view.findViewById(R.id.bannerViewPager);
        dotsIndicator = view.findViewById(R.id.dotsIndicator);
        bannerProgressBar = view.findViewById(R.id.bannerProgressBar);

        // Movie Sections
        limitedFreeTitleTextView = view.findViewById(R.id.limitedFreeTitleTextView);
        limitedFreeRecyclerView = view.findViewById(R.id.limitedFreeRecyclerView);

        topShowsTitleTextView = view.findViewById(R.id.topShowsTitleTextView);
        topShowsRecyclerView = view.findViewById(R.id.topShowsRecyclerView);

        premiumContentTitleTextView = view.findViewById(R.id.premiumContentTitleTextView);
        premiumContentRecyclerView = view.findViewById(R.id.premiumContentRecyclerView);
    }

    /**
     * Tải các drawable cần thiết cho toolbar background
     */
    private void loadDrawables() {
        toolbarBackgroundDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.toolbar_background_gradient);
        defaultToolbarBackgroundDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.toolbar_background);
    }

    /**
     * Thiết lập hiệu ứng background cho toolbar khi cuộn sử dụng drawable
     */
    private void setupToolbarScroll() {
        // Thiết lập ban đầu: toolbar trong suốt
        toolbar.setBackground(defaultToolbarBackgroundDrawable);

        // Lắng nghe sự kiện cuộn của AppBarLayout
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                // Tính toán tỷ lệ cuộn (0.0 đến 1.0)
                float scrollRatio = Math.abs(verticalOffset) / (float) appBarLayout.getTotalScrollRange();

                // Áp dụng background drawable khi đạt ngưỡng cuộn (ví dụ: 15%)
                if (scrollRatio >= 0.15) {
                    // Hiển thị background drawer khi cuộn quá ngưỡng
                    toolbar.setBackground(toolbarBackgroundDrawable);

                    // Thêm hiệu ứng elevation khi cuộn gần hết
                    if (scrollRatio >= 0.95) {
                        toolbar.setElevation(4.0f);
                    } else {
                        toolbar.setElevation(0.0f);
                    }
                } else {
                    // Khi cuộn ít hơn ngưỡng, toolbar trong suốt
                    toolbar.setBackground(defaultToolbarBackgroundDrawable);
                    toolbar.setElevation(0.0f);
                }
            }
        });
    }

    private void setupClickListeners() {
        vipButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "VIP Button Clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTabLayout() {
        String[] categories = {"For You", "Drama", "Comedy", "Romance", "Action", "Thriller", "Horror"};
        for (String category : categories) {
            categoryTabLayout.addTab(categoryTabLayout.newTab().setText(category));
        }
        if (categoryTabLayout.getTabCount() > 0) {
            Objects.requireNonNull(categoryTabLayout.getTabAt(0)).select();
        }
        categoryTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String selectedCategory = tab.getText() != null ? tab.getText().toString() : "";
                Toast.makeText(getContext(), "Selected Category: " + selectedCategory, Toast.LENGTH_SHORT).show();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupAdaptersAndLayouts() {
        // Setup Banner ViewPager2
        bannerAdapter = new BannerAdapter(requireContext());
        bannerViewPager.setAdapter(bannerAdapter);
        dotsIndicator.attachTo(bannerViewPager);

        // Setup Movie RecyclerViews
        limitedFreeAdapter = new MovieHomeAdapter(requireContext());
        limitedFreeRecyclerView.setAdapter(limitedFreeAdapter);
        limitedFreeRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        limitedFreeRecyclerView.setHasFixedSize(true);

        topShowsAdapter = new MovieHomeAdapter(requireContext());
        topShowsRecyclerView.setAdapter(topShowsAdapter);
        topShowsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        topShowsRecyclerView.setHasFixedSize(true);

        premiumContentAdapter = new MovieHomeAdapter(requireContext());
        premiumContentRecyclerView.setAdapter(premiumContentAdapter);
        premiumContentRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        premiumContentRecyclerView.setHasFixedSize(true);
    }

    private void observeViewModelData() {
        // Observe Banner data
        bannerViewModel.banners.observe(getViewLifecycleOwner(), banners -> {
            stopAutoScroll();
            boolean hasBanners = banners != null && !banners.isEmpty();
            if (hasBanners) {
                bannerAdapter.setBanners(banners);
            }
            bannerViewPager.setVisibility(hasBanners ? View.VISIBLE : View.GONE);
            dotsIndicator.setVisibility(hasBanners ? View.VISIBLE : View.GONE);

            if (hasBanners && banners.size() > 1) {
                startAutoScroll();
            }
        });

        // Observe Banner loading state
        bannerViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                bannerProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                if (isLoading) {
                    stopAutoScroll();
                    bannerViewPager.setVisibility(View.INVISIBLE);
                    dotsIndicator.setVisibility(View.INVISIBLE);
                } else {
                    boolean hasBanners = bannerViewModel.banners.getValue() != null && !bannerViewModel.banners.getValue().isEmpty();
                    if(hasBanners) {
                        bannerViewPager.setVisibility(View.VISIBLE);
                        dotsIndicator.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        // Observe Banner errors
        bannerViewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "Banner Loading Error: " + error, Toast.LENGTH_LONG).show();
                bannerViewPager.setVisibility(View.GONE);
                dotsIndicator.setVisibility(View.GONE);
                bannerProgressBar.setVisibility(View.GONE);
                stopAutoScroll();
            }
        });
    }

    private void fetchInitialBanners() {
        bannerViewModel.fetchBanners(0, 5, null, null);
    }

    private void startAutoScroll() {
        if (autoScrollHandler == null) {
            autoScrollHandler = new Handler(Looper.getMainLooper());
        }
        if (autoScrollRunnable == null) {
            autoScrollRunnable = () -> {
                if (bannerAdapter == null || bannerAdapter.getItemCount() <= 1) return;
                int currentItem = bannerViewPager.getCurrentItem();
                int nextItem = (currentItem + 1) % bannerAdapter.getItemCount();
                bannerViewPager.setCurrentItem(nextItem, true);
            };
        }

        stopAutoScroll();

        autoScrollTimer = new Timer();
        autoScrollTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (autoScrollHandler != null && autoScrollRunnable != null) {
                    autoScrollHandler.post(autoScrollRunnable);
                }
            }
        }, AUTO_SCROLL_DELAY, AUTO_SCROLL_PERIOD);

        bannerViewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        bannerViewPager.registerOnPageChangeCallback(pageChangeCallback);
    }

    private void stopAutoScroll() {
        if (autoScrollTimer != null) {
            autoScrollTimer.cancel();
            autoScrollTimer = null;
        }
    }

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
            if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                isUserDragging = true;
                stopAutoScroll();
            } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                if (isUserDragging) {
                    isUserDragging = false;
                }
            }
        }
    };
}