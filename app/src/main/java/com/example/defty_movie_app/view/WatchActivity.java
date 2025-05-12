package com.example.defty_movie_app.view;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff; // THÊM IMPORT NÀY
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer; // THÊM IMPORT NÀY
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.CastCrewAdapter;
import com.example.defty_movie_app.adapter.RecommendedMovieAdapter;
import com.example.defty_movie_app.data.dto.DownloadedMovie;
import com.example.defty_movie_app.data.model.adapter.CastCrew;
import com.example.defty_movie_app.data.model.response.EpisodeResponse;
import com.example.defty_movie_app.data.model.response.MovieDetailResponse;
import com.example.defty_movie_app.data.model.response.RecommendedMovieResponse;
import com.example.defty_movie_app.data.remote.AuthApiService;
import com.example.defty_movie_app.data.remote.RecommenderServiceApi;
import com.example.defty_movie_app.data.repository.AuthRepository;
import com.example.defty_movie_app.data.repository.CallRecommender;
import com.example.defty_movie_app.utils.GridSpacingItemDecoration;
import com.example.defty_movie_app.utils.LocaleHelper;
import com.example.defty_movie_app.viewmodel.DownloadViewModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchActivity extends AppCompatActivity {
    private RecyclerView recyclerViewRecommended;
    private RecommendedMovieAdapter adapter;
    private ProgressBar progressBar;
    private VideoView videoView;
    private TextView textTitle, textDescription;
    private ImageView coverImage;
    private ImageButton btnPlay;
    private String episodeUrl;
    private ScrollView scrollContent;
    private View commentList;
    private View commentInputBox;
    private TextView textDescription1;
    private TextView btnToggleDescription;
    private RecyclerView recyclerView;
    private Integer movieId;

    private ImageButton iconDownloadMovieButton;
    private DownloadViewModel downloadViewModel;
    private static final int REQUEST_CODE_DOWNLOAD_PERMISSION = 201;
    private DownloadedMovie pendingMovieToDownload;
    private String currentMovieSlug;
    private String currentMovieTitle;
    private String currentCoverImageUrl;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("app_lang", "en");
        Context context = LocaleHelper.wrap(newBase, lang);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        videoView = findViewById(R.id.videoView);
        textTitle = findViewById(R.id.textTitle);
        textDescription = findViewById(R.id.textDescription);
        coverImage = findViewById(R.id.coverImage);
        btnPlay = findViewById(R.id.btnPlay);
        scrollContent = findViewById(R.id.scrollContent);
        commentList = findViewById(R.id.commentList);
        commentInputBox = findViewById(R.id.commentInputBox);
        textDescription1 = findViewById(R.id.textDescription1);
        btnToggleDescription = findViewById(R.id.btnToggleDescription);
        recyclerView = findViewById(R.id.recyclerCastCrew);

        recyclerViewRecommended = findViewById(R.id.recyclerViewRecommended);
        progressBar = findViewById(R.id.progressBar);

        recyclerViewRecommended.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerViewRecommended.setHasFixedSize(true);
        recyclerViewRecommended.addItemDecoration(new GridSpacingItemDecoration(3, 16));

        adapter = new RecommendedMovieAdapter(new ArrayList<>());
        recyclerViewRecommended.setAdapter(adapter);

        iconDownloadMovieButton = findViewById(R.id.iconDownloadMovieButton);
        downloadViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);

        String slug = getIntent().getStringExtra("MOVIE_SLUG_ID");
        currentMovieSlug = slug;

        if (slug != null && !slug.isEmpty()) {
            fetchEpisode(slug);
            fetchMovieDetail(slug); // Trong fetchMovieDetail, sau khi có movieId và slug, ta sẽ gọi loadDownloadedMovies
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin phim", Toast.LENGTH_SHORT).show();
            if (iconDownloadMovieButton != null) {
                iconDownloadMovieButton.setEnabled(false);
            }
        }

        // Quan sát LiveData từ ViewModel để cập nhật trạng thái nút download
        downloadViewModel.getDownloadedMoviesLiveData().observe(this, new Observer<List<DownloadedMovie>>() {
            @Override
            public void onChanged(List<DownloadedMovie> downloadedMovies) {
                updateDownloadButtonState(); // Gọi hàm cập nhật khi danh sách thay đổi
            }
        });


        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (episodeUrl != null && !episodeUrl.isEmpty()) {
                    Uri videoUri = Uri.parse(episodeUrl);
                    videoView.setVideoURI(videoUri);
                    MediaController mediaController = new MediaController(WatchActivity.this);
                    mediaController.setAnchorView(videoView);
                    videoView.setMediaController(mediaController);
                    btnPlay.setVisibility(View.GONE);
                    coverImage.setVisibility(View.GONE);
                    videoView.setVisibility(View.VISIBLE);
                    getWindow().setFlags(
                            WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN
                    );
                    videoView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    );
                    videoView.requestFocus();
                    videoView.start();
                } else {
                    Toast.makeText(WatchActivity.this, "Link video không khả dụng", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnToggleDescription.setOnClickListener(new View.OnClickListener() {
            boolean expanded = false;
            @Override
            public void onClick(View v) {
                if (expanded) {
                    textDescription1.setMaxLines(1);
                    textDescription1.setEllipsize(TextUtils.TruncateAt.END);
                    btnToggleDescription.setText("Xem thêm");
                } else {
                    textDescription1.setMaxLines(Integer.MAX_VALUE);
                    textDescription1.setEllipsize(null);
                    btnToggleDescription.setText("Ẩn bớt");
                }
                expanded = !expanded;
            }
        });

        scrollContent.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (commentList == null || scrollContent == null || commentInputBox == null) return;
            int[] scrollViewLocation = new int[2];
            scrollContent.getLocationOnScreen(scrollViewLocation);
            int[] commentListLocation = new int[2];
            commentList.getLocationOnScreen(commentListLocation);
            int scrollY = commentListLocation[1] - scrollViewLocation[1];
            int scrollViewHeight = scrollContent.getHeight();
            if (scrollY >= 0 && scrollY <= scrollViewHeight) {
                commentInputBox.setVisibility(View.VISIBLE);
            } else {
                commentInputBox.setVisibility(View.GONE);
            }
        });
        scrollContent.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (commentList == null || scrollContent == null || commentInputBox == null) return;
            float commentListY = commentList.getY() - scrollContent.getScrollY();
            int scrollHeight = scrollContent.getHeight();
            if (commentListY >= 0 && commentListY <= scrollHeight) {
                commentInputBox.setVisibility(View.VISIBLE);
            } else {
                commentInputBox.setVisibility(View.GONE);
            }
        });

        if (iconDownloadMovieButton != null) {
            iconDownloadMovieButton.setOnClickListener(v -> handleDownloadClick());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Làm mới danh sách tải về khi activity resume để cập nhật trạng thái nút
        if (downloadViewModel != null) {
            downloadViewModel.loadDownloadedMovies();
        }
    }

    private void updateDownloadButtonState() {
        if (iconDownloadMovieButton == null || movieId == null || TextUtils.isEmpty(currentMovieSlug)) {
            // Nếu chưa có đủ thông tin phim hiện tại, không làm gì cả hoặc reset về mặc định
            if(iconDownloadMovieButton != null) {
                iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_default_tint), PorterDuff.Mode.SRC_IN);
                // Hoặc nếu bạn muốn dùng icon khác: iconDownloadMovieButton.setImageResource(R.drawable.ic_download);
            }
            return;
        }

        List<DownloadedMovie> allDownloads = downloadViewModel.getDownloadedMoviesLiveData().getValue();
        DownloadedMovie currentMovieInList = null;
        if (allDownloads != null) {
            for (DownloadedMovie downloadedMovie : allDownloads) {
                // So sánh cả movieId và slug để chắc chắn đúng là tập phim/phim đó
                if (downloadedMovie.getId() == movieId && currentMovieSlug.equals(downloadedMovie.getSlug())) {
                    currentMovieInList = downloadedMovie;
                    break;
                }
            }
        }

        if (currentMovieInList != null && DownloadedMovie.STATUS_COMPLETED.equals(currentMovieInList.getDownloadStatus())) {
            iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_completed_green), PorterDuff.Mode.SRC_IN);
            // Tùy chọn: thay đổi icon nếu muốn, ví dụ:
            // iconDownloadMovieButton.setImageResource(R.drawable.ic_download_done_custom); // Tạo icon này nếu muốn
        } else {
            iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_default_tint), PorterDuff.Mode.SRC_IN);
            // Tùy chọn: đặt lại icon mặc định
            // iconDownloadMovieButton.setImageResource(R.drawable.ic_download); // Icon tải xuống mặc định của bạn
        }
    }


    private void fetchMovieDetail(String slug) {
        AuthApiService apiService = AuthRepository.getInstance().getApi();
        apiService.getMovieDetail(slug).enqueue(new Callback<MovieDetailResponse>() {
            @Override
            public void onResponse(Call<MovieDetailResponse> call, Response<MovieDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    MovieDetailResponse.Movie movie = response.body().data;
                    movieId = movie.id;
                    currentMovieTitle = movie.title;
                    currentCoverImageUrl = movie.coverImage;

                    if (movieId != null) {
                        fetchRecommendedMovies(movieId);
                    } else {
                        if(progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                    textTitle.setText(movie.title);
                    String rating = movie.rating != null ? "★ " + movie.rating : "No rating";
                    String year = movie.releaseDate != null && movie.releaseDate.length() >= 4
                            ? movie.releaseDate.substring(0, 4)
                            : "Unknown";
                    String description = rating + " | " + year;
                    textDescription.setText(description);
                    textDescription1.setText(movie.description);
                    String imageUrl = movie.coverImage;
                    if (!isFinishing() && imageUrl != null) {
                        Glide.with(WatchActivity.this).load(imageUrl).into(coverImage);
                    }
                    List<CastCrew> castList = new ArrayList<>();
                    if (movie.director != null) {
                        castList.add(new CastCrew(movie.director.getName(), "Director", movie.director.getThumbnail()));
                    }
                    if (movie.actor != null) {
                        for (MovieDetailResponse.Actor actor : movie.actor) {
                            castList.add(new CastCrew(actor.getName(), "Actor", actor.getAvatar()));
                        }
                    }
                    CastCrewAdapter castAdapter = new CastCrewAdapter(castList);
                    if (recyclerView != null) {
                        recyclerView.setLayoutManager(new LinearLayoutManager(WatchActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        recyclerView.setAdapter(castAdapter);
                    }
                    // Sau khi thông tin phim (movieId, slug) đã có, gọi loadDownloadedMovies để trigger updateDownloadButtonState
                    if(downloadViewModel != null) {
                        downloadViewModel.loadDownloadedMovies();
                    }

                } else {
                    if(progressBar != null) progressBar.setVisibility(View.GONE);
                    Log.e("WatchActivity", "Lỗi fetchMovieDetail hoặc data null: " + response.code() + " - " + response.message());
                    Toast.makeText(WatchActivity.this, "Lỗi tải chi tiết phim.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<MovieDetailResponse> call, Throwable t) {
                if(progressBar != null) progressBar.setVisibility(View.GONE);
                Log.e("WatchActivity", "Lỗi khi gọi API fetchMovieDetail", t);
                Toast.makeText(WatchActivity.this, "Lỗi API chi tiết phim.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchEpisode(String slug){
        AuthApiService apiService = AuthRepository.getInstance().getApi();
        apiService.getEpisode(slug).enqueue(new Callback<EpisodeResponse>() {
            @Override
            public void onResponse(Call<EpisodeResponse> call, Response<EpisodeResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    EpisodeResponse.Episode episode = response.body().data;
                    episodeUrl = episode.getLink();
                } else {
                    Log.e("WatchActivity", "Lỗi fetchEpisode hoặc data null: " + response.code() + " - " + response.message());
                    Toast.makeText(WatchActivity.this, "Lỗi tải link tập phim.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<EpisodeResponse> call, Throwable t) {
                Log.e("WatchActivity", "Lỗi khi gọi API fetchEpisode", t);
                Toast.makeText(WatchActivity.this, "Lỗi API link tập phim.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchRecommendedMovies(Integer movieIdToFetch){
        if (movieIdToFetch == null) {
            if(progressBar != null) progressBar.setVisibility(View.GONE);
            Log.e("WatchActivity", "movieIdToFetch là null, không thể fetch recommended movies.");
            return;
        }
        if(progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if(recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE);

        RecommenderServiceApi apiService = CallRecommender.getInstance().getApi();
        apiService.getRecommendedMovie(movieIdToFetch).enqueue(new Callback<RecommendedMovieResponse>() {
            @Override
            public void onResponse(Call<RecommendedMovieResponse> call, Response<RecommendedMovieResponse> response) {
                if(progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<RecommendedMovieResponse.RecommendedMovie> recommendedMovies = response.body().similarMovies;
                    if (recommendedMovies != null && !recommendedMovies.isEmpty()) {
                        Log.d("Recommended", "Số lượng phim: " + recommendedMovies.size());
                        if(recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.VISIBLE);
                        if(adapter != null) adapter.updateMovies(recommendedMovies);
                    } else {
                        Log.d("WatchActivity", "Danh sách phim trống");
                        if(recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE);
                    }
                }
                else {
                    Log.d("WatchActivity", "Dữ liệu API trống hoặc lỗi: " + response.code() + " - " + response.message());
                    if(recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(Call<RecommendedMovieResponse> call, Throwable t) {
                if(progressBar != null) progressBar.setVisibility(View.GONE);
                if(recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE);
                Log.e("WatchActivity", "Lỗi khi gọi API fetchRecommendedMovies", t);
                Toast.makeText(WatchActivity.this, "Lỗi API phim đề xuất.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleDownloadClick() {
        if (movieId == null || TextUtils.isEmpty(currentMovieTitle) ||
                TextUtils.isEmpty(currentCoverImageUrl) || TextUtils.isEmpty(episodeUrl) ||
                TextUtils.isEmpty(currentMovieSlug)) {
            Toast.makeText(this, "Thông tin phim chưa sẵn sàng để tải. Vui lòng thử lại sau.", Toast.LENGTH_LONG).show();
            Log.d("DownloadInfo", "Thông tin còn thiếu để tải: movieId=" + movieId + ", title=" + currentMovieTitle + ", cover=" + currentCoverImageUrl + ", episodeUrl=" + episodeUrl + ", slug=" + currentMovieSlug);
            return;
        }
        DownloadedMovie movieToDownload = new DownloadedMovie(
                movieId,
                currentMovieTitle,
                currentCoverImageUrl,
                episodeUrl,
                currentMovieSlug
        );
        pendingMovieToDownload = movieToDownload;
        if (checkAndRequestStoragePermission()) {
            boolean downloadWillActuallyStart = downloadViewModel.startDownload(pendingMovieToDownload); // ViewModel của bạn cần trả về boolean
            if (downloadWillActuallyStart) {
                Toast.makeText(this, "Đang chuẩn bị tải: " + pendingMovieToDownload.getTitle(), Toast.LENGTH_SHORT).show();
            }
            pendingMovieToDownload = null;
        }
    }
    private boolean checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_DOWNLOAD_PERMISSION);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_DOWNLOAD_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền lưu trữ.", Toast.LENGTH_SHORT).show();
                if (pendingMovieToDownload != null) {
                    boolean downloadWillActuallyStart = downloadViewModel.startDownload(pendingMovieToDownload); // ViewModel của bạn cần trả về boolean
                    if (downloadWillActuallyStart) {
                        Toast.makeText(this, "Đang chuẩn bị tải: " + pendingMovieToDownload.getTitle(), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Quyền lưu trữ bị từ chối. Không thể tải phim.", Toast.LENGTH_LONG).show();
            }
            pendingMovieToDownload = null;
        }
    }
}