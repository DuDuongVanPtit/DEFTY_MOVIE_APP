package com.example.defty_movie_app.view;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.MovieAdapter;
import com.example.defty_movie_app.data.dto.Category;
import com.example.defty_movie_app.data.model.response.ShowonResponse;
import com.example.defty_movie_app.viewmodel.LibraryViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends AppCompatActivity {
    private MovieAdapter adapter;
    private LibraryViewModel libraryViewModel;
    private LinearLayout containerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        // Khởi tạo TabLayout và RecyclerView
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        adapter = new MovieAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        // Khởi tạo ViewModel
        libraryViewModel = new ViewModelProvider(this).get(LibraryViewModel.class);

        // Quan sát LiveData từ ViewModel để cập nhật danh sách ShowonResponse (danh sách các tab)
        libraryViewModel.getShowonData().observe(this, showonList -> {
            if (showonList != null && !showonList.isEmpty()) {
                // Kiểm tra nếu tab đã được setup, tránh gọi nhiều lần
                if (tabLayout.getTabCount() == 0) {
                    setupTabsWithData(tabLayout, showonList);
                }
            }
        });

        // Quan sát kết quả từ ViewModel để cập nhật danh sách phim
        libraryViewModel.getMovies().observe(this, movies -> {
            // Cập nhật danh sách phim vào Adapter
            if (movies != null) {
                adapter.setMovies(movies);
            }
        });

        // Fetch dữ liệu từ API nếu chưa có
        if (libraryViewModel.getShowonData().getValue() == null) {
            libraryViewModel.fetchShowons(0, 10, null, null, null);
        }

        // Load dữ liệu giả (nếu cần)
        libraryViewModel.getMovies();

        // Tạo containerLayout để chứa categories
        containerLayout = findViewById(R.id.containerLayout);

        // Tạo dữ liệu giả cho categories
        List<Category> fakeCategories = getFakeCategories();

        // Tạo view cho mỗi category
        for (Category category : fakeCategories) {
            createCategoryView(category);
        }
    }

    private void setupTabsWithData(TabLayout tabLayout, List<ShowonResponse> showonList) {
        tabLayout.removeAllTabs();  // Xóa tất cả các tab cũ trước khi thêm tab mới

        // Tạo các tab từ danh sách showonList
        for (ShowonResponse showon : showonList) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setText(showon.getContentName());  // Dùng contentName làm tên tab
            tab.setTag(showon);  // Gán đối tượng ShowonResponse vào tab
            tabLayout.addTab(tab);
        }

        // Lắng nghe khi tab được chọn
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                ShowonResponse selectedShowon = (ShowonResponse) tab.getTag();  // Lấy ShowonResponse từ tag
                loadMoviesForTab(selectedShowon);  // Load dữ liệu cho tab được chọn
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadMoviesForTab(ShowonResponse selectedShowon) {
        // Lấy contentType từ ShowonResponse và gọi ViewModel để fetch dữ liệu
        String contentType = selectedShowon.getContentType();  // Dữ liệu động từ ShowonResponse

        // Gọi ViewModel để load dữ liệu cho tab được chọn
        libraryViewModel.fetchShowons(0, 10, contentType, null, null);
    }

    private List<Category> getFakeCategories() {
        List<Category> categories = new ArrayList<>();

        // Category 1
        List<String> regions = new ArrayList<>();
        regions.add("Region 1");
        regions.add("Region 2");
        regions.add("Region 3");
        categories.add(new Category("All Regions", regions));

        // Category 2
        List<String> categoriesList = new ArrayList<>();
        categoriesList.add("Category 1");
        categoriesList.add("Category 2");
        categoriesList.add("Category 3");
        categories.add(new Category("All Categories", categoriesList));

        return categories;
    }

    private void createCategoryView(Category category) {
        TextView categoryTitle = getTextView(category);

        // Tạo HorizontalScrollView cho các item
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        LinearLayout scrollLayout = new LinearLayout(this);
        scrollLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Tạo các button cho các items và áp dụng style
        for (String item : category.getItems()) {
            Button button = new Button(this);
            button.setText(item);
            button.setTextColor(getResources().getColor(R.color.text_category));
            button.setBackgroundResource(R.drawable.button_border);
            button.setTextSize(14);

            button.setMinHeight(0);
            button.setMinimumHeight(0);
            button.setMinWidth(0);
            button.setMinimumWidth(0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            button.setLayoutParams(params);

            // Click listener
            button.setOnClickListener(v -> onCategorySelected(item));

            scrollLayout.addView(button);
        }

        scrollView.addView(scrollLayout);
        containerLayout.addView(categoryTitle);
        containerLayout.addView(scrollView);
    }

    @NonNull
    private TextView getTextView(Category category) {
        TextView categoryTitle = new TextView(this);
        categoryTitle.setText(category.getName());
        categoryTitle.setTextColor(Color.WHITE);
        categoryTitle.setTextSize(16f);
        categoryTitle.setPadding(8, 8, 8, 8);

        // Thêm margin bằng LayoutParams
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(16, 24, 16, 8); // left, top, right, bottom
        categoryTitle.setLayoutParams(titleParams);
        return categoryTitle;
    }

    private void onCategorySelected(String category) {
        // Xử lý khi chọn một category
        System.out.println("Category selected: " + category);

        // Load phim cho category đã chọn
        libraryViewModel.fetchMoviesByCategory(0, 10, category);
    }
}
