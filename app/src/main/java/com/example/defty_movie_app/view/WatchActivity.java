package com.example.defty_movie_app.view;

import android.Manifest;
import android.content.DialogInterface; // Import cho AlertDialog
import android.content.Intent;
import android.content.pm.ActivityInfo;
// import android.content.res.Configuration; // Không dùng trực tiếp trong code này nữa
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
// import android.view.ViewGroup; // Không dùng trực tiếp
// import android.view.WindowManager; // Không dùng trực tiếp
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog; // Import cho AlertDialog
import androidx.appcompat.app.AppCompatActivity;
// import androidx.constraintlayout.widget.ConstraintLayout;
// import androidx.core.view.WindowCompat; // Không dùng trực tiếp
// import androidx.core.view.WindowInsetsCompat; // Không dùng trực tiếp
// import androidx.core.view.WindowInsetsControllerCompat; // Không dùng trực tiếp
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
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
import com.example.defty_movie_app.viewmodel.DownloadViewModel;

import java.util.ArrayList;
import java.util.List;
// import java.util.Objects; // Không dùng trực tiếp

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchActivity extends AppCompatActivity {

    private static final String TAG = "WatchActivity";

    private TextView textTitle, textDescription, textDescription1, btnToggleDescription;
    private ImageView coverImage;
    private ImageButton btnPlay; // Nút play ban đầu, sẽ bị ẩn đi
    private ScrollView scrollContent;
    private View commentList;
    private View commentInputBox;
    private RecyclerView recyclerViewCastCrew;
    private ProgressBar progressBarRecommended; // Đổi tên từ progressBar để khớp với code bạn gửi
    private RecyclerView recyclerViewRecommended;
    private RecommendedMovieAdapter recommendedAdapter; // Đổi tên từ adapter để khớp

    private PlayerView playerView;
    private ExoPlayer player;
    private String episodeUrl;
    private ActivityResultLauncher<Intent> fullscreenLauncher;
    private long startPositionToResume = 0; // Lưu vị trí khi quay lại từ fullscreen

    // --- Biến cho các nút điều khiển tùy chỉnh ---
    private ImageButton btnPlayCustom;
    private ImageButton btnPauseCustom;
    private Player.Listener playerListener; // Listener để cập nhật UI nút play/pause
    // --- ---

    private Integer movieId;

    private ImageButton iconDownloadMovieButton;
    private DownloadViewModel downloadViewModel;
    private static final int REQUEST_CODE_DOWNLOAD_PERMISSION = 201;
    private DownloadedMovie pendingMovieToDownload;
    private String currentMovieSlug;
    private String currentMovieTitle;
    private String currentCoverImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        findViews();
        setupRecyclerViews();

        fullscreenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        long lastPosition = result.getData().getLongExtra(FullscreenPlayerActivity.RESULT_LAST_POSITION, 0);
                        Log.d(TAG, "Returned from fullscreen at position: " + lastPosition);

                        startPositionToResume = lastPosition; // Luôn cập nhật startPositionToResume

                        if (player == null) { // Nếu player đã bị release (ví dụ do onStop)
                            Log.d(TAG, "Player was null, re-initializing.");
                            initializePlayer(); // Hàm này sẽ tạo player, set media, prepare, và seek
                            // và play nếu btnPlay ban đầu bị ẩn
                        } else { // Player vẫn còn tồn tại
                            Log.d(TAG, "Player exists, seeking and playing.");
                            player.seekTo(lastPosition);
                            player.play(); // Tiếp tục phát
                        }
                        playerView.setVisibility(View.VISIBLE); // Đảm bảo PlayerView hiển thị

                    } else {
                        Log.d(TAG, "Returned from fullscreen without RESULT_OK or data, current player state: " + (player != null ? player.getPlaybackState() : "null"));
                        // Nếu người dùng chỉ back ra mà không có kết quả rõ ràng, vẫn thử resume nếu player còn
                        if (player != null) {
                            player.play();
                        }
                    }
                    // Đảm bảo màn hình quay lại Portrait sau khi thoát fullscreen
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                });

        iconDownloadMovieButton = findViewById(R.id.iconDownloadMovieButton);
        downloadViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);


        String slug = getIntent().getStringExtra("MOVIE_SLUG_ID");
        currentMovieSlug = slug;
        if (slug == null || slug.isEmpty()) {
            Log.e(TAG, "Movie slug is missing!");
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin phim.", Toast.LENGTH_LONG).show();
            if (iconDownloadMovieButton != null) {
                iconDownloadMovieButton.setEnabled(false);
            }
            finish();
            return;
        }

        if (iconDownloadMovieButton != null) {
            iconDownloadMovieButton.setOnClickListener(v -> handleDownloadClick());
        }

        fetchMovieDetail(slug);
        fetchEpisode(slug);
        setupListeners();

        // Quan sát LiveData từ ViewModel để cập nhật trạng thái nút download
        downloadViewModel.getDownloadedMoviesLiveData().observe(this, new Observer<List<DownloadedMovie>>() {
            @Override
            public void onChanged(List<DownloadedMovie> downloadedMovies) {
                updateDownloadButtonState(); // Gọi hàm cập nhật khi danh sách thay đổi
            }
        });
    }

    private void findViews() {
        playerView = findViewById(R.id.player_view);
        coverImage = findViewById(R.id.coverImage);
        btnPlay = findViewById(R.id.btnPlay);
        textTitle = findViewById(R.id.textTitle);
        textDescription = findViewById(R.id.textDescription);
        textDescription1 = findViewById(R.id.textDescription1);
        btnToggleDescription = findViewById(R.id.btnToggleDescription);
        recyclerViewCastCrew = findViewById(R.id.recyclerCastCrew);
        recyclerViewRecommended = findViewById(R.id.recyclerViewRecommended);
        progressBarRecommended = findViewById(R.id.progressBar); // Khớp với tên biến của bạn
        scrollContent = findViewById(R.id.scrollContent);
        commentList = findViewById(R.id.commentList);
        commentInputBox = findViewById(R.id.commentInputBox);
    }

    private void setupRecyclerViews() {
        recyclerViewCastCrew.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewCastCrew.setHasFixedSize(true);

        recyclerViewRecommended.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerViewRecommended.setHasFixedSize(true);
        try {
            recyclerViewRecommended.addItemDecoration(new GridSpacingItemDecoration(3, 16));
        } catch (Exception e) {
            Log.w(TAG, "GridSpacingItemDecoration error.", e);
        }
        recommendedAdapter = new RecommendedMovieAdapter(new ArrayList<>()); // Khớp với tên biến
        recyclerViewRecommended.setAdapter(recommendedAdapter); // Khớp với tên biến
    }

    private void setupListeners() {
        btnPlay.setOnClickListener(v -> playVideo()); // Nút play ban đầu
        btnToggleDescription.setOnClickListener(new View.OnClickListener() { /* ... như cũ ... */
            boolean expanded = false;
            @Override
            public void onClick(View v) {
                if (expanded) {
                    textDescription1.setMaxLines(1);
                    textDescription1.setEllipsize(TextUtils.TruncateAt.END);
                    btnToggleDescription.setText(R.string.view_more);
                } else {
                    textDescription1.setMaxLines(Integer.MAX_VALUE);
                    textDescription1.setEllipsize(null);
                    btnToggleDescription.setText(R.string.hide);
                }
                expanded = !expanded;
            }
        });
    }

    private void playVideo() {
        if (episodeUrl != null && !episodeUrl.isEmpty()) {
            initializePlayer();
            if (player != null) {
                try {
                    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(episodeUrl));
                    player.setMediaItem(mediaItem);
                    player.prepare();
                    player.play();
                    btnPlay.setVisibility(View.GONE);
                    coverImage.setVisibility(View.GONE);
                    playerView.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting media item or playing video", e);
                    Toast.makeText(this, "Lỗi khi phát video.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Player is null after initialization attempt.");
                Toast.makeText(this, "Không thể khởi tạo trình phát.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "Episode URL is not available yet.");
            Toast.makeText(this, "Đang tải dữ liệu video...", Toast.LENGTH_SHORT).show();
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer() {
        if (player == null) { // Chỉ khởi tạo nếu player đang là null
            if (episodeUrl == null || episodeUrl.isEmpty()) {
                Log.e(TAG, "Cannot initialize player: episodeUrl is null or empty.");
                Toast.makeText(this, "Không có URL video để phát.", Toast.LENGTH_SHORT).show();
                return; // Không thể khởi tạo nếu không có URL
            }
            try {
                player = new ExoPlayer.Builder(this).build();
                playerView.setPlayer(player);

                initializePlayerListener(); // Hàm bạn đã tạo để khởi tạo Player.Listener
                player.addListener(playerListener); // Thêm listener vào player

                setupCustomControlListeners(); // Thiết lập các nút điều khiển tùy chỉnh

                // --- QUAN TRỌNG: Thiết lập MediaItem và Prepare ---
                MediaItem mediaItem = MediaItem.fromUri(Uri.parse(episodeUrl));
                player.setMediaItem(mediaItem);
                player.prepare(); // Chuẩn bị player
                // --- ---

                if (startPositionToResume > 0) {
                    player.seekTo(startPositionToResume);
                    Log.d(TAG, "Player initialized and resumed from: " + startPositionToResume);
                    startPositionToResume = 0; // Reset sau khi seek
                } else {
                    Log.d(TAG, "Player initialized.");
                }

                // Quyết định có nên tự động phát hay không
                // Nếu nút play ban đầu đã bị ẩn, nghĩa là người dùng đã từng nhấn play
                if (btnPlay.getVisibility() == View.GONE) {
                    player.play();
                }
                updatePlayPauseButtons(player.isPlaying()); // Cập nhật UI nút play/pause

            } catch (Exception e) {
                Log.e(TAG, "Error initializing ExoPlayer", e);
                Toast.makeText(this, "Lỗi khởi tạo trình phát", Toast.LENGTH_SHORT).show();
            }
        } else if (startPositionToResume > 0) { // Player đã tồn tại, chỉ cần seek
            // Đảm bảo player đã prepare nếu vì lý do nào đó nó ở trạng thái IDLE
            if (player.getPlaybackState() == Player.STATE_IDLE) {
                if (episodeUrl != null && !episodeUrl.isEmpty()) {
                    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(episodeUrl));
                    player.setMediaItem(mediaItem);
                    player.prepare();
                } else {
                    Log.e(TAG, "Cannot seek, episodeUrl is missing for existing player.");
                    return;
                }
            }
            player.seekTo(startPositionToResume);
            Log.d(TAG, "Player resumed from: " + startPositionToResume);
            startPositionToResume = 0; // Reset sau khi seek
            // Việc gọi player.play() sẽ được xử lý bởi ActivityResultCallback hoặc onResume
        }
        // Cập nhật lại trạng thái nút play/pause nếu player đã tồn tại
        if (player != null) {
            updatePlayPauseButtons(player.isPlaying());
        }
    }


    // Hàm mới để khởi tạo Player.Listener
    private void initializePlayerListener() {
        playerListener = new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseButtons(isPlaying);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if(player != null) {
                    updatePlayPauseButtons(player.isPlaying());
                }
            }
        };
    }

    // Hàm mới để cập nhật visibility của nút play/pause
    private void updatePlayPauseButtons(boolean isPlaying) {
        if (btnPlayCustom != null && btnPauseCustom != null) {
            btnPlayCustom.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
            btnPauseCustom.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
        }
    }


    private void setupCustomControlListeners() {
        if (playerView == null) return;

        // Tìm nút bằng ID mới
        btnPlayCustom = playerView.findViewById(R.id.btn_play_custom);
        btnPauseCustom = playerView.findViewById(R.id.btn_pause_custom);
        ImageButton btnRewind = playerView.findViewById(R.id.btn_rewind_custom);
        ImageButton btnFfwd = playerView.findViewById(R.id.btn_ffwd_custom);
        ImageButton btnFullscreen = playerView.findViewById(R.id.btn_fullscreen_custom);
        ImageButton btnSettings = playerView.findViewById(R.id.btn_settings_custom);

        if (btnPlayCustom != null) {
            btnPlayCustom.setOnClickListener(v -> {
                if (player != null) player.play();
            });
        } else {
            Log.w(TAG,"Custom Play button (btn_play_custom) not found!");
        }

        if (btnPauseCustom != null) {
            btnPauseCustom.setOnClickListener(v -> {
                if (player != null) player.pause();
            });
        } else {
            Log.w(TAG,"Custom Pause button (btn_pause_custom) not found!");
        }

        if (btnRewind != null) {
            btnRewind.setOnClickListener(v -> handleRewind());
        } else {
            Log.w(TAG,"Rewind button (btn_rewind_custom) not found!");
        }

        if (btnFfwd != null) {
            btnFfwd.setOnClickListener(v -> handleFastForward());
        } else {
            Log.w(TAG,"Fast Forward button (btn_ffwd_custom) not found!");
        }

        if (btnFullscreen != null) {
            btnFullscreen.setOnClickListener(v -> openFullscreenActivity());
        } else {
            Log.w(TAG,"Fullscreen button (btn_fullscreen_custom) not found!");
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> handleSettings());
        } else {
            Log.w(TAG,"Settings button (btn_settings_custom) not found!");
        }
    }

    private void openFullscreenActivity() {
        if (player != null && episodeUrl != null && !episodeUrl.isEmpty()) {
            long currentPosition = player.getCurrentPosition();
            player.pause();

            Intent intent = new Intent(this, FullscreenPlayerActivity.class);
            intent.putExtra(FullscreenPlayerActivity.EXTRA_VIDEO_URL, episodeUrl);
            intent.putExtra(FullscreenPlayerActivity.EXTRA_START_POSITION, currentPosition);

            Log.d(TAG, "Launching fullscreen from position: " + currentPosition);
            fullscreenLauncher.launch(intent);
        } else {
            Log.w(TAG, "Cannot open fullscreen: Player or URL not ready.");
            Toast.makeText(this, "Trình phát chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleRewind() {
        if (player != null) {
            long currentPosition = player.getCurrentPosition();
            player.seekTo(Math.max(0, currentPosition - 10000));
        }
    }

    private void handleFastForward() {
        if (player != null) {
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            if (duration != androidx.media3.common.C.TIME_UNSET) {
                player.seekTo(Math.min(duration, currentPosition + 10000));
            }
        }
    }

    // Thêm hàm handleSettings vào WatchActivity
    private void handleSettings() {
        if (player == null) {
            Toast.makeText(this, "Trình phát chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
            return;
        }
        final CharSequence[] speedOptions = {"0.5x", "0.75x", "Bình thường (1x)", "1.25x", "1.5x", "2x"};
        final float[] speedValues = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        float currentSpeed = player.getPlaybackParameters().speed;
        int currentSpeedIndex = -1;
        for (int i = 0; i < speedValues.length; i++) {
            if (Math.abs(speedValues[i] - currentSpeed) < 0.01f) {
                currentSpeedIndex = i;
                break;
            }
        }
        if (currentSpeedIndex == -1) currentSpeedIndex = 2;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn tốc độ phát");
        builder.setSingleChoiceItems(speedOptions, currentSpeedIndex, (dialog, which) -> {
            float selectedSpeed = speedValues[which];
            if (player != null) {
                player.setPlaybackParameters(new PlaybackParameters(selectedSpeed));
            }
            dialog.dismiss();
            Toast.makeText(WatchActivity.this, "Tốc độ: " + speedOptions[which], Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    // --- Quản lý Vòng đời ExoPlayer ---
    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24 && player == null) {
            if(episodeUrl != null && !episodeUrl.isEmpty()){
                initializePlayer();
            }
        }
    }
    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onResume() {
        super.onResume();
        // Khởi tạo player nếu cần (ví dụ, sau khi bị release ở onStop cho API < 24, hoặc nếu Activity mới được tạo)
        if (player == null && episodeUrl != null && !episodeUrl.isEmpty()) {
            Log.d(TAG, "onResume: Player is null, initializing.");
            initializePlayer(); // Hàm initializePlayer sẽ xử lý việc seek đến startPositionToResume nếu có
        }
        // Nếu player đã tồn tại (hoặc vừa được khởi tạo)
        if (player != null) {
            // Chỉ tự động play nếu nút play ban đầu đã bị ẩn (nghĩa là người dùng đã chủ động play video trước đó)
            // và player đang không phát (ví dụ, do bị pause ở onPause)
            if (btnPlay.getVisibility() == View.GONE) {
                Log.d(TAG, "onResume: Resuming playback.");
                player.play();
            }
            updatePlayPauseButtons(player.isPlaying()); // Luôn cập nhật UI nút
        }

        if (downloadViewModel != null) {
            downloadViewModel.loadDownloadedMovies();
        }
    }
    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) { // Luôn lưu vị trí và pause nếu player tồn tại
            startPositionToResume = player.getCurrentPosition(); // Lưu vị trí để resume
            player.pause();
        }
        // if (Util.SDK_INT < 24 && player != null) { // Logic cũ, có thể không cần thiết nữa
        //     player.pause();
        // }
    }
    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() { // Bỏ @OptIn ở đây vì không dùng Util.SDK_INT trực tiếp
        super.onDestroy();
        // Luôn release player ở onDestroy cho mọi API level để tránh leak
        // if (Util.SDK_INT < 24) { // Không cần check này nữa, releasePlayer sẽ làm
        //     releasePlayer();
        // }
        releasePlayer(); // Luôn gọi ở onDestroy
    }

    private void releasePlayer() {
        if (player != null) {
            if (playerListener != null) {
                player.removeListener(playerListener); // Xóa listener
            }
            startPositionToResume = player.getCurrentPosition(); // Lưu vị trí cuối cùng trước khi release
            player.release();
            player = null;
            playerView.setPlayer(null);
            Log.d(TAG, "WatchActivity player released. Last position: " + startPositionToResume);
            // Sau khi release, có thể hiện lại nút play ban đầu nếu muốn
            // btnPlay.setVisibility(View.VISIBLE);
            // coverImage.setVisibility(View.VISIBLE);
            // if (btnPlayCustom != null) btnPlayCustom.setVisibility(View.VISIBLE);
            // if (btnPauseCustom != null) btnPauseCustom.setVisibility(View.GONE);
        }
    }

    // --- Các hàm gọi API (Giữ nguyên của bạn, có thể đã sửa một chút ở lần trước) ---
    private void fetchMovieDetail(String slug) { /* ... như code bạn cung cấp hoặc đã sửa ... */
        AuthApiService apiService = AuthRepository.getInstance().getApi();
        apiService.getMovieDetail(slug).enqueue(new Callback<MovieDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieDetailResponse> call, @NonNull Response<MovieDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    MovieDetailResponse.Movie movie = response.body().data;
                    movieId = movie.id;
                    currentMovieTitle = movie.title;
                    currentCoverImageUrl = movie.coverImage;
                    textTitle.setText(movie.title);
                    String rating = movie.rating != null ? "★ " + movie.rating : "N/A";
                    String year = movie.releaseDate != null && movie.releaseDate.length() >= 4 ? movie.releaseDate.substring(0, 4) : "N/A";
                    textDescription.setText(String.format("%s | %s", rating, year));
                    textDescription1.setText(movie.description);
                    if(coverImage.getVisibility() == View.VISIBLE) {
                        Glide.with(WatchActivity.this).load(movie.coverImage).placeholder(R.drawable.default_cover_image).error(R.drawable.default_cover_image).into(coverImage);
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
                    recyclerViewCastCrew.setAdapter(castAdapter);
                    if (movieId != null) {
                        fetchRecommendedMovies(movieId);
                    }

                    // Sau khi thông tin phim (movieId, slug) đã có, gọi loadDownloadedMovies để trigger updateDownloadButtonState
                    if(downloadViewModel != null) {
                        downloadViewModel.loadDownloadedMovies();
                    }

                } else {
                    Log.e(TAG, "fetchMovieDetail - Response error. Code: " + response.code());
                    Toast.makeText(WatchActivity.this, "Không lấy được chi tiết phim", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<MovieDetailResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchMovieDetail - API call failed", t);
                Toast.makeText(WatchActivity.this, "Lỗi mạng khi tải chi tiết phim", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchEpisode(String slug) { /* ... như code bạn cung cấp hoặc đã sửa ... */
        AuthApiService apiService = AuthRepository.getInstance().getApi();
        apiService.getEpisode(slug).enqueue(new Callback<EpisodeResponse>() {
            @Override
            public void onResponse(@NonNull Call<EpisodeResponse> call, @NonNull Response<EpisodeResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    EpisodeResponse.Episode episode = response.body().data;
                    episodeUrl = episode.getLink();
                    Log.d(TAG, "Episode URL fetched: " + episodeUrl);
                    btnPlay.setEnabled(true);
                } else {
                    Log.e(TAG, "fetchEpisode - Response error. Code: " + response.code());
                    Toast.makeText(WatchActivity.this, "Không tải được link video", Toast.LENGTH_SHORT).show();
                    btnPlay.setEnabled(false);
                }
            }
            @Override
            public void onFailure(@NonNull Call<EpisodeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchEpisode - API call failed", t);
                Toast.makeText(WatchActivity.this, "Lỗi mạng khi tải link video", Toast.LENGTH_SHORT).show();
                btnPlay.setEnabled(false);
            }
        });
    }

    private void fetchRecommendedMovies(Integer movieId) { /* ... như code bạn cung cấp hoặc đã sửa ... */
        if (movieId == null) return;
        progressBarRecommended.setVisibility(View.VISIBLE); // Khớp tên biến
        recyclerViewRecommended.setVisibility(View.GONE);
        RecommenderServiceApi apiService = CallRecommender.getInstance().getApi();
        apiService.getRecommendedMovie(movieId).enqueue(new Callback<RecommendedMovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecommendedMovieResponse> call, @NonNull Response<RecommendedMovieResponse> response) {
                progressBarRecommended.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().similarMovies != null) {
                    List<RecommendedMovieResponse.RecommendedMovie> recommendedMovies = response.body().similarMovies;
                    if (recommendedMovies != null && !recommendedMovies.isEmpty()) {
                        Log.d(TAG, "Recommended movies: " + recommendedMovies.size());
                        recommendedAdapter.updateMovies(recommendedMovies); // Khớp tên biến
                        recyclerViewRecommended.setVisibility(View.VISIBLE);
                    } else {
                        Log.d(TAG, "No recommended movies found.");
                        recyclerViewRecommended.setVisibility(View.GONE);
                        Toast.makeText(WatchActivity.this, "Không có phim đề xuất", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "fetchRecommendedMovies - Response error. Code: " + response.code());
                    Toast.makeText(WatchActivity.this, "Lỗi khi tải phim đề xuất", Toast.LENGTH_SHORT).show();
                    recyclerViewRecommended.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(@NonNull Call<RecommendedMovieResponse> call, @NonNull Throwable t) {
                progressBarRecommended.setVisibility(View.GONE);
                recyclerViewRecommended.setVisibility(View.GONE);
                Log.e(TAG, "fetchRecommendedMovies - API call failed", t);
                Toast.makeText(WatchActivity.this, "Lỗi mạng khi tải phim đề xuất", Toast.LENGTH_SHORT).show();
            }
        });
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