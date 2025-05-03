package com.example.defty_movie_app.view;

import android.net.Uri;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.CastCrewAdapter;
import com.example.defty_movie_app.adapter.RecommendedMovieAdapter;
import com.example.defty_movie_app.data.model.adapter.CastCrew;
import com.example.defty_movie_app.data.model.response.EpisodeResponse;
import com.example.defty_movie_app.data.model.response.MovieDetailResponse;
import com.example.defty_movie_app.data.model.response.RecommendedMovieResponse;
import com.example.defty_movie_app.data.remote.AuthApiService;
import com.example.defty_movie_app.data.remote.RecommenderServiceApi;
import com.example.defty_movie_app.data.repository.AuthRepository;
import com.example.defty_movie_app.data.repository.CallRecommender;
import com.example.defty_movie_app.utils.GridSpacingItemDecoration;

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

        //recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        // Khởi tạo RecyclerView và ProgressBar
        recyclerViewRecommended = findViewById(R.id.recyclerViewRecommended);
        progressBar = findViewById(R.id.progressBar);

        // Thiết lập GridLayoutManager và Adapter
        recyclerViewRecommended.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerViewRecommended.setHasFixedSize(true);
        recyclerViewRecommended.addItemDecoration(new GridSpacingItemDecoration(3, 16));
        //recyclerViewRecommended.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Khởi tạo Adapter với danh sách rỗng
        adapter = new RecommendedMovieAdapter(new ArrayList<>());
        recyclerViewRecommended.setAdapter(adapter);

        fetchEpisode("nguoi-phan-xu-37af2me1");
        fetchMovieDetail("nguoi-phan-xu-37af2me1");

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (episodeUrl != null && !episodeUrl.isEmpty()) {
                    Uri videoUri = Uri.parse(episodeUrl);
                    videoView.setVideoURI(videoUri);

                    // Cài MediaController
                    MediaController mediaController = new MediaController(WatchActivity.this);
                    mediaController.setAnchorView(videoView);
                    videoView.setMediaController(mediaController);

                    // Ẩn nút play và ảnh cover, hiển thị video
                    btnPlay.setVisibility(View.GONE);
                    coverImage.setVisibility(View.GONE);
                    videoView.setVisibility(View.VISIBLE);

                    // Fullscreen mode
                    getWindow().setFlags(
                            WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN
                    );

                    // Ẩn thanh điều hướng (navigation bar) nếu muốn
                    videoView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    );

                    // Phát video
                    videoView.requestFocus();
                    videoView.start();
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
        });scrollContent.getViewTreeObserver().addOnScrollChangedListener(() -> {
            float commentListY = commentList.getY() - scrollContent.getScrollY();
            int scrollHeight = scrollContent.getHeight();

            if (commentListY >= 0 && commentListY <= scrollHeight) {
                commentInputBox.setVisibility(View.VISIBLE);
            } else {
                commentInputBox.setVisibility(View.GONE);
            }
        });
    }

    private void fetchMovieDetail(String slug) {

        AuthApiService apiService = AuthRepository.getInstance().getApi();

        apiService.getMovieDetail(slug).enqueue(new Callback<MovieDetailResponse>() {
            @Override
            public void onResponse(Call<MovieDetailResponse> call, Response<MovieDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MovieDetailResponse.Movie movie = response.body().data;
                    movieId = movie.id;
                    fetchRecommendedMovies(movieId);
                    textTitle.setText(movie.title);
                    String rating = movie.rating != null ? "★ " + movie.rating : "No rating";
                    String year = movie.releaseDate != null && movie.releaseDate.length() >= 4
                            ? movie.releaseDate.substring(0, 4)
                            : "Unknown";

                    String description = rating + " | " + year;
                    textDescription.setText(description);

                    textDescription1.setText(movie.description);


                    String imageUrl = movie.coverImage;
                    Glide.with(WatchActivity.this).load(imageUrl).into(coverImage);

                    List<CastCrew> castList = new ArrayList<>();

                    if (movie.director != null) {
                        castList.add(new CastCrew(movie.director.getName(), "Director", movie.director.getThumbnail()));
                    }

                    for (MovieDetailResponse.Actor actor : movie.actor) {
                        castList.add(new CastCrew(actor.getName(), "Actor", actor.getAvatar()));
                    }

                    CastCrewAdapter adapter = new CastCrewAdapter(castList);
                    recyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<MovieDetailResponse> call, Throwable t) {
                Log.e("WatchActivity", "Lỗi khi gọi API", t);
            }
        });
    }

    private void fetchEpisode(String slug){
        AuthApiService apiService = AuthRepository.getInstance().getApi();

        apiService.getEpisode(slug).enqueue(new Callback<EpisodeResponse>() {
            @Override
            public void onResponse(Call<EpisodeResponse> call, Response<EpisodeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EpisodeResponse.Episode episode = response.body().data;
                    episodeUrl = episode.getLink();

                }
            }

            @Override
            public void onFailure(Call<EpisodeResponse> call, Throwable t) {
                Log.e("WatchActivity", "Lỗi khi gọi API", t);
            }
        });
    }
    private void fetchRecommendedMovies(Integer  movieId){
        RecommenderServiceApi apiService = CallRecommender.getInstance().getApi();

        apiService.getRecommendedMovie(movieId).enqueue(new Callback<RecommendedMovieResponse>() {
            @Override
            public void onResponse(Call<RecommendedMovieResponse> call, Response<RecommendedMovieResponse> response) {
                progressBar.setVisibility(View.GONE);
                recyclerViewRecommended.setVisibility(View.VISIBLE);
                if (response.isSuccessful() && response.body() != null) {
                    List<RecommendedMovieResponse.RecommendedMovie> recommendedMovies = response.body().similarMovies;
                    Log.d("Recommended", "Số lượng phim: " + recommendedMovies.size());
                    adapter.updateMovies(recommendedMovies);
//                    if (recommendedMovies != null && !recommendedMovies.isEmpty()) {
//                        adapter = new RecommendedMovieAdapter(recommendedMovies);
//                        recyclerViewRecommended.setAdapter(adapter);
//                    } else {
//                        Log.d("WatchActivity", "Danh sách phim trống");
//                    }
                }
                else {
                    Log.d("WatchActivity", "Dữ liệu API trống hoặc lỗi");
                    Toast.makeText(WatchActivity.this, "Không có phim đề xuất", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RecommendedMovieResponse> call, Throwable t) {
                Log.e("WatchActivity", "Lỗi khi gọi API", t);
                // Có thể hiển thị thông báo lỗi cho người dùng, ví dụ: Toast
                Toast.makeText(WatchActivity.this, "Lỗi khi tải danh sách phim", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

