package com.example.defty_movie_app.view;

import android.Manifest; // --- DOWNLOAD ---
import android.content.Context; // --- DOWNLOAD ---
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences; // --- DOWNLOAD ---
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager; // --- DOWNLOAD ---
import android.graphics.PorterDuff; // --- DOWNLOAD ---
import android.net.Uri;
import android.os.Build; // --- DOWNLOAD ---
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat; // --- DOWNLOAD ---
import androidx.core.content.ContextCompat; // --- DOWNLOAD ---
import androidx.lifecycle.Observer; // --- DOWNLOAD ---
import androidx.lifecycle.ViewModelProvider; // --- DOWNLOAD ---
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
import com.example.defty_movie_app.adapter.EpisodeAdapter;
import com.example.defty_movie_app.adapter.RecommendedMovieAdapter;
import com.example.defty_movie_app.data.dto.DownloadedMovie; // --- DOWNLOAD ---
import com.example.defty_movie_app.data.model.adapter.CastCrew;
import com.example.defty_movie_app.data.model.response.EpisodeResponse;
import com.example.defty_movie_app.data.model.response.MovieDetailResponse;
import com.example.defty_movie_app.data.model.response.RecommendedMovieResponse;
import com.example.defty_movie_app.data.remote.AuthApiService;
import com.example.defty_movie_app.data.remote.RecommenderServiceApi;
import com.example.defty_movie_app.data.repository.AuthRepository;
import com.example.defty_movie_app.data.repository.CallRecommender;
import com.example.defty_movie_app.utils.GridSpacingItemDecoration;
import com.example.defty_movie_app.utils.LocaleHelper; // --- DOWNLOAD --- (assuming LocaleHelper is used)
import com.example.defty_movie_app.viewmodel.DownloadViewModel; // --- DOWNLOAD ---

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchActivity extends AppCompatActivity implements EpisodeAdapter.OnEpisodeClickListener{

    private static final String TAG = "WatchActivity";

    private TextView textTitle, textDescription, textDescription1, btnToggleDescription;
    private ImageView coverImage;
    private ImageButton btnPlay; // Nút play ban đầu, sẽ bị ẩn đi
    private ScrollView scrollContent;
    private View commentList;
    private View commentInputBox;
    private RecyclerView recyclerViewCastCrew;
    private ProgressBar progressBarRecommended;
    private RecyclerView recyclerViewRecommended;
    private RecommendedMovieAdapter recommendedAdapter;

    private PlayerView playerView;
    private ExoPlayer player;
    private String episodeUrl;
    private ActivityResultLauncher<Intent> fullscreenLauncher;
    private long startPositionToResume = 0;

    private ImageButton btnPlayCustom;
    private ImageButton btnPauseCustom;
    private Player.Listener playerListener;

    private Integer movieId;

    // --- DOWNLOAD: Variables ---
    private ImageButton iconDownloadMovieButton;
    private DownloadViewModel downloadViewModel;
    private static final int REQUEST_CODE_DOWNLOAD_PERMISSION = 201;
    private DownloadedMovie pendingMovieToDownload;
    private String currentMovieSlug;
    private String currentMovieTitle;
    private String currentCoverImageUrl;
    // --- DOWNLOAD: End Variables ---

    private RecyclerView recyclerViewEpisodes; // Thêm RecyclerView cho tập phim
    private EpisodeAdapter episodeAdapter;     // Thêm Adapter cho tập phim
    private String currentPlayingEpisodeSlug;  // Lưu slug của tập đang phát
    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("app_lang", "en"); // Default to English
        Context context = LocaleHelper.wrap(newBase, lang);
        super.attachBaseContext(context);
    }
    // --- DOWNLOAD: End Localization ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details); // Make sure this layout has iconDownloadMovieButton
        findViews();
        setupRecyclerViews();

        fullscreenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        long lastPosition = result.getData().getLongExtra(FullscreenPlayerActivity.RESULT_LAST_POSITION, 0);
                        Log.d(TAG, "Returned from fullscreen at position: " + lastPosition);
                        startPositionToResume = lastPosition;
                        if (player == null) {
                            Log.d(TAG, "Player was null, re-initializing.");
                            initializePlayer();
                        } else {
                            Log.d(TAG, "Player exists, seeking and playing.");
                            player.seekTo(lastPosition);
                            player.play();
                        }
                        playerView.setVisibility(View.VISIBLE);
                    } else {
                        Log.d(TAG, "Returned from fullscreen without RESULT_OK or data, current player state: " + (player != null ? player.getPlaybackState() : "null"));
                        if (player != null) {
                            player.play();
                        }
                    }
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                });

        String slug = getIntent().getStringExtra("MOVIE_SLUG_ID");
        currentMovieSlug = slug; // --- DOWNLOAD: Store slug ---

        if (slug == null || slug.isEmpty()) {
            Log.e(TAG, "Movie slug is missing!");
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin phim.", Toast.LENGTH_LONG).show();
            if (iconDownloadMovieButton != null) { // --- DOWNLOAD ---
                iconDownloadMovieButton.setEnabled(false); // --- DOWNLOAD ---
            } // --- DOWNLOAD ---
            finish();
            return;
        }

        // --- DOWNLOAD: Initialize DownloadViewModel and observe LiveData ---
        downloadViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);
        downloadViewModel.getDownloadedMoviesLiveData().observe(this, new Observer<List<DownloadedMovie>>() {
            @Override
            public void onChanged(List<DownloadedMovie> downloadedMovies) {
                updateDownloadButtonState();
            }
        });
        // --- DOWNLOAD: End Initialization ---

        fetchMovieDetail(slug); // This will also trigger loadDownloadedMovies once movieId is available
        fetchEpisode(slug);     // This will set episodeUrl, important for download
        setupListeners();
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
        progressBarRecommended = findViewById(R.id.progressBar);
        scrollContent = findViewById(R.id.scrollContent);
        commentList = findViewById(R.id.commentList); // Assuming these exist in your layout
        commentInputBox = findViewById(R.id.commentInputBox); // Assuming these exist

        // --- DOWNLOAD: Find download button ---
        iconDownloadMovieButton = findViewById(R.id.iconDownloadMovieButton); // Make sure this ID exists in R.layout.activity_movie_details
        if (iconDownloadMovieButton == null) {
            Log.e(TAG, "iconDownloadMovieButton not found in layout. Download functionality will be affected.");
        }
        // --- DOWNLOAD: End find download button ---

        recyclerViewEpisodes = findViewById(R.id.recyclerViewEpisodes);
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
        recommendedAdapter = new RecommendedMovieAdapter(new ArrayList<>());
        recyclerViewRecommended.setAdapter(recommendedAdapter);

        recyclerViewEpisodes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewEpisodes.setHasFixedSize(true);
        episodeAdapter = new EpisodeAdapter(this, this);
        recyclerViewEpisodes.setAdapter(episodeAdapter);
    }

    private void setupListeners() {
        btnPlay.setOnClickListener(v -> playVideo());
        btnToggleDescription.setOnClickListener(new View.OnClickListener() {
            boolean expanded = false;
            @Override
            public void onClick(View v) {
                if (expanded) {
                    textDescription1.setMaxLines(1);
                    textDescription1.setEllipsize(TextUtils.TruncateAt.END);
                    btnToggleDescription.setText(getString(R.string.view_more)); // Use string resource
                } else {
                    textDescription1.setMaxLines(Integer.MAX_VALUE);
                    textDescription1.setEllipsize(null);
                    btnToggleDescription.setText(getString(R.string.hide)); // Use string resource
                }
                expanded = !expanded;
            }
        });

        // --- DOWNLOAD: Set listener for download button ---
        if (iconDownloadMovieButton != null) {
            iconDownloadMovieButton.setOnClickListener(v -> handleDownloadClick());
        }
        // --- DOWNLOAD: End set listener ---

        // Optional: Scroll listener for comment box (if needed from original snippet)
        if (scrollContent != null && commentList != null && commentInputBox != null) {
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
        }
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
        if (player == null) {
            if (episodeUrl == null || episodeUrl.isEmpty()) {
                Log.e(TAG, "Cannot initialize player: episodeUrl is null or empty.");
                Toast.makeText(this, "Không có URL video để phát.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                player = new ExoPlayer.Builder(this).build();
                playerView.setPlayer(player);
                initializePlayerListener();
                player.addListener(playerListener);
                setupCustomControlListeners();

                MediaItem mediaItem = MediaItem.fromUri(Uri.parse(episodeUrl));
                player.setMediaItem(mediaItem);
                player.prepare();

                if (startPositionToResume > 0) {
                    player.seekTo(startPositionToResume);
                    Log.d(TAG, "Player initialized and resumed from: " + startPositionToResume);
                    startPositionToResume = 0;
                } else {
                    Log.d(TAG, "Player initialized.");
                }

                if (btnPlay.getVisibility() == View.GONE) {
                    player.play();
                }
                updatePlayPauseButtons(player.isPlaying());

            } catch (Exception e) {
                Log.e(TAG, "Error initializing ExoPlayer", e);
                Toast.makeText(this, "Lỗi khởi tạo trình phát", Toast.LENGTH_SHORT).show();
            }
        } else if (startPositionToResume > 0) {
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
            startPositionToResume = 0;
        }
        if (player != null) {
            updatePlayPauseButtons(player.isPlaying());
        }
    }


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

    private void updatePlayPauseButtons(boolean isPlaying) {
        if (btnPlayCustom != null && btnPauseCustom != null) {
            btnPlayCustom.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
            btnPauseCustom.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
        }
    }


    private void setupCustomControlListeners() {
        if (playerView == null) return;
        btnPlayCustom = playerView.findViewById(R.id.btn_play_custom);
        btnPauseCustom = playerView.findViewById(R.id.btn_pause_custom);
        ImageButton btnRewind = playerView.findViewById(R.id.btn_rewind_custom);
        ImageButton btnFfwd = playerView.findViewById(R.id.btn_ffwd_custom);
        ImageButton btnFullscreen = playerView.findViewById(R.id.btn_fullscreen_custom);
        ImageButton btnSettings = playerView.findViewById(R.id.btn_settings_custom);

        if (btnPlayCustom != null) btnPlayCustom.setOnClickListener(v -> { if (player != null) player.play(); });
        else Log.w(TAG,"Custom Play button (btn_play_custom) not found!");
        if (btnPauseCustom != null) btnPauseCustom.setOnClickListener(v -> { if (player != null) player.pause(); });
        else Log.w(TAG,"Custom Pause button (btn_pause_custom) not found!");
        if (btnRewind != null) btnRewind.setOnClickListener(v -> handleRewind());
        else Log.w(TAG,"Rewind button (btn_rewind_custom) not found!");
        if (btnFfwd != null) btnFfwd.setOnClickListener(v -> handleFastForward());
        else Log.w(TAG,"Fast Forward button (btn_ffwd_custom) not found!");
        if (btnFullscreen != null) btnFullscreen.setOnClickListener(v -> openFullscreenActivity());
        else Log.w(TAG,"Fullscreen button (btn_fullscreen_custom) not found!");
        if (btnSettings != null) btnSettings.setOnClickListener(v -> handleSettings());
        else Log.w(TAG,"Settings button (btn_settings_custom) not found!");
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
        if (player != null) player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
    }

    private void handleFastForward() {
        if (player != null) {
            long duration = player.getDuration();
            if (duration != androidx.media3.common.C.TIME_UNSET) {
                player.seekTo(Math.min(duration, player.getCurrentPosition() + 10000));
            }
        }
    }

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
        if (currentSpeedIndex == -1) currentSpeedIndex = 2; // Default to 1x

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn tốc độ phát");
        builder.setSingleChoiceItems(speedOptions, currentSpeedIndex, (dialog, which) -> {
            if (player != null) player.setPlaybackParameters(new PlaybackParameters(speedValues[which]));
            dialog.dismiss();
            Toast.makeText(WatchActivity.this, "Tốc độ: " + speedOptions[which], Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }


    // --- ExoPlayer Lifecycle Management ---
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
        if (player == null && episodeUrl != null && !episodeUrl.isEmpty()) {
            Log.d(TAG, "onResume: Player is null, initializing.");
            initializePlayer();
        }
        if (player != null) {
            if (btnPlay.getVisibility() == View.GONE) { // Only resume if user explicitly started play before
                Log.d(TAG, "onResume: Resuming playback.");
                player.play();
            }
            updatePlayPauseButtons(player.isPlaying());
        }
        // --- DOWNLOAD: Refresh download list on resume ---
        if (downloadViewModel != null) {
            downloadViewModel.loadDownloadedMovies();
        }
        // --- DOWNLOAD: End refresh ---
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            startPositionToResume = player.getCurrentPosition();
            player.pause();
        }
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
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer(); // Always release player in onDestroy
    }

    private void releasePlayer() {
        if (player != null) {
            if (playerListener != null) player.removeListener(playerListener);
            startPositionToResume = player.getCurrentPosition();
            player.release();
            player = null;
            if (playerView != null) playerView.setPlayer(null); // Check playerView for null
            Log.d(TAG, "WatchActivity player released. Last position: " + startPositionToResume);
        }
    }

    // --- API Fetching Methods ---
    private void fetchMovieDetail(String slug) {
        AuthApiService apiService = AuthRepository.getInstance().getApi();
        apiService.getMovieDetail(slug).enqueue(new Callback<MovieDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieDetailResponse> call, @NonNull Response<MovieDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    MovieDetailResponse.Movie movie = response.body().data;
                    movieId = movie.id;
                    // --- DOWNLOAD: Store movie title and cover image ---
                    currentMovieTitle = movie.title;
                    currentCoverImageUrl = movie.coverImage;
                    // --- DOWNLOAD: End store ---

                    textTitle.setText(movie.title);
                    String rating = movie.rating != null ? "★ " + movie.rating : "N/A";
                    String year = movie.releaseDate != null && movie.releaseDate.length() >= 4 ? movie.releaseDate.substring(0, 4) : "N/A";
                    textDescription.setText(String.format("%s | %s", rating, year));
                    textDescription1.setText(movie.description);

                    if(coverImage != null && coverImage.getVisibility() == View.VISIBLE) { // Check coverImage for null
                        Glide.with(WatchActivity.this)
                                .load(movie.coverImage)
                                .placeholder(R.drawable.default_cover_image) // Ensure this drawable exists
                                .error(R.drawable.default_cover_image)       // Ensure this drawable exists
                                .into(coverImage);
                    }

                    if (movie.episode != null && !movie.episode.isEmpty()) {
                        episodeAdapter.updateEpisodes(movie.episode);
                        // Nếu currentPlayingEpisodeSlug chưa được set (lần đầu load),
                        // và fetchEpisode đã chạy và có slug của tập đầu tiên,
                        // thì set nó ở đây. Hoặc set nó ngay sau khi fetchEpisode thành công.
                        if (currentPlayingEpisodeSlug != null) {
                            episodeAdapter.setCurrentPlayingEpisode(currentPlayingEpisodeSlug);
                        } else if (!movie.episode.isEmpty()){
                            // Mặc định chọn tập đầu tiên nếu chưa có gì đang phát
                            // (Điều này cần phối hợp với fetchEpisode)
                            // currentPlayingEpisodeSlug = movie.episode.get(0).getSlug();
                            // episodeAdapter.setCurrentPlayingEpisode(currentPlayingEpisodeSlug);
                        }
                    }

                    List<CastCrew> castList = new ArrayList<>();
                    if (movie.director != null) castList.add(new CastCrew(movie.director.getName(), "Director", movie.director.getThumbnail()));
                    if (movie.actor != null) {
                        for (MovieDetailResponse.Actor actor : movie.actor) {
                            castList.add(new CastCrew(actor.getName(), "Actor", actor.getAvatar()));
                        }
                    }
                    if (recyclerViewCastCrew != null) { // Check recyclerView for null
                        CastCrewAdapter castAdapter = new CastCrewAdapter(castList);
                        recyclerViewCastCrew.setAdapter(castAdapter);
                    }

                    if (movieId != null) {
                        fetchRecommendedMovies(movieId);
                    }

                    // --- DOWNLOAD: Load downloaded movies to update button state ---
                    if (downloadViewModel != null && movieId != null && !TextUtils.isEmpty(currentMovieSlug)) {
                        downloadViewModel.loadDownloadedMovies();
                    }
                    // --- DOWNLOAD: End load ---

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

    private void fetchEpisode(String slug) {
        AuthApiService apiService = AuthRepository.getInstance().getApi();
        apiService.getEpisode(slug).enqueue(new Callback<EpisodeResponse>() {
            @Override
            public void onResponse(@NonNull Call<EpisodeResponse> call, @NonNull Response<EpisodeResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    EpisodeResponse.Episode episode = response.body().data;
                    episodeUrl = episode.getLink(); // episodeUrl is crucial for download too
                    if (episode.getSlug() != null) { // Model EpisodeResponse.Episode cần có getSlug()
                        currentPlayingEpisodeSlug = episode.getSlug();
                        Log.d(TAG, "Initial playing episode slug: " + currentPlayingEpisodeSlug);
                        // Cập nhật adapter nếu nó đã có dữ liệu
                        if (episodeAdapter != null && episodeAdapter.getItemCount() > 0) {
                            episodeAdapter.setCurrentPlayingEpisode(currentPlayingEpisodeSlug);
                        }
                    }
                    Log.d(TAG, "Episode URL fetched: " + episodeUrl);
                    if (btnPlay != null) btnPlay.setEnabled(true); // Check btnPlay for null
                } else {
                    Log.e(TAG, "fetchEpisode - Response error. Code: " + response.code());
                    Toast.makeText(WatchActivity.this, "Không tải được link video", Toast.LENGTH_SHORT).show();
                    if (btnPlay != null) btnPlay.setEnabled(false);
                }
            }
            @Override
            public void onFailure(@NonNull Call<EpisodeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchEpisode - API call failed", t);
                Toast.makeText(WatchActivity.this, "Lỗi mạng khi tải link video", Toast.LENGTH_SHORT).show();
                if (btnPlay != null) btnPlay.setEnabled(false);
            }
        });
    }

    private void fetchRecommendedMovies(Integer movieIdToFetch) {
        if (movieIdToFetch == null) return;
        if (progressBarRecommended != null) progressBarRecommended.setVisibility(View.VISIBLE); // Check for null
        if (recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE); // Check for null

        RecommenderServiceApi apiService = CallRecommender.getInstance().getApi();
        apiService.getRecommendedMovie(movieIdToFetch).enqueue(new Callback<RecommendedMovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecommendedMovieResponse> call, @NonNull Response<RecommendedMovieResponse> response) {
                if (progressBarRecommended != null) progressBarRecommended.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().similarMovies != null) {
                    List<RecommendedMovieResponse.RecommendedMovie> recommendedMovies = response.body().similarMovies;
                    if (recommendedMovies != null && !recommendedMovies.isEmpty()) {
                        Log.d(TAG, "Recommended movies: " + recommendedMovies.size());
                        if (recommendedAdapter != null) recommendedAdapter.updateMovies(recommendedMovies); // Check for null
                        if (recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.VISIBLE);
                    } else {
                        Log.d(TAG, "No recommended movies found.");
                        if (recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE);
                        // Toast.makeText(WatchActivity.this, "Không có phim đề xuất", Toast.LENGTH_SHORT).show(); // Optional
                    }
                } else {
                    Log.e(TAG, "fetchRecommendedMovies - Response error. Code: " + response.code());
                    // Toast.makeText(WatchActivity.this, "Lỗi khi tải phim đề xuất", Toast.LENGTH_SHORT).show(); // Optional
                    if (recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(@NonNull Call<RecommendedMovieResponse> call, @NonNull Throwable t) {
                if (progressBarRecommended != null) progressBarRecommended.setVisibility(View.GONE);
                if (recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE);
                Log.e(TAG, "fetchRecommendedMovies - API call failed", t);
                // Toast.makeText(WatchActivity.this, "Lỗi mạng khi tải phim đề xuất", Toast.LENGTH_SHORT).show(); // Optional
            }
        });
    }

    // --- DOWNLOAD: Methods ---
    private void updateDownloadButtonState() {
        if (iconDownloadMovieButton == null || movieId == null || TextUtils.isEmpty(currentMovieSlug)) {
            if (iconDownloadMovieButton != null) {
                iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_default_tint), PorterDuff.Mode.SRC_IN);
                // Or: iconDownloadMovieButton.setImageResource(R.drawable.ic_download_default);
            }
            return;
        }

        List<DownloadedMovie> allDownloads = downloadViewModel.getDownloadedMoviesLiveData().getValue();
        DownloadedMovie currentMovieInList = null;
        if (allDownloads != null) {
            for (DownloadedMovie downloadedMovie : allDownloads) {
                if (downloadedMovie.getId() == movieId && currentMovieSlug.equals(downloadedMovie.getSlug())) {
                    currentMovieInList = downloadedMovie;
                    break;
                }
            }
        }

        if (currentMovieInList != null && DownloadedMovie.STATUS_COMPLETED.equals(currentMovieInList.getDownloadStatus())) {
            iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_completed_green), PorterDuff.Mode.SRC_IN);
            // Or: iconDownloadMovieButton.setImageResource(R.drawable.ic_download_done);
        } else {
            iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_default_tint), PorterDuff.Mode.SRC_IN);
            // Or: iconDownloadMovieButton.setImageResource(R.drawable.ic_download_default);
        }
    }

    private void handleDownloadClick() {
        if (movieId == null || TextUtils.isEmpty(currentMovieTitle) ||
                TextUtils.isEmpty(currentCoverImageUrl) || TextUtils.isEmpty(episodeUrl) || // episodeUrl is important!
                TextUtils.isEmpty(currentMovieSlug)) {
            Toast.makeText(this, "Thông tin phim chưa sẵn sàng để tải. Vui lòng thử lại sau.", Toast.LENGTH_LONG).show();
            Log.d("DownloadInfo", "Thông tin còn thiếu để tải: movieId=" + movieId +
                    ", title=" + currentMovieTitle + ", cover=" + currentCoverImageUrl +
                    ", episodeUrl=" + episodeUrl + ", slug=" + currentMovieSlug);
            return;
        }

        DownloadedMovie movieToDownload = new DownloadedMovie(
                movieId,
                currentMovieTitle,
                currentCoverImageUrl,
                episodeUrl, // Use the fetched episodeUrl
                currentMovieSlug
        );
        pendingMovieToDownload = movieToDownload;

        if (checkAndRequestStoragePermission()) {
            // Permission already granted or not needed (Android Q+ with Scoped Storage for app-specific dir)
            boolean downloadWillActuallyStart = downloadViewModel.startDownload(pendingMovieToDownload);
            if (downloadWillActuallyStart) { // Assuming startDownload returns a boolean
                Toast.makeText(this, "Đang chuẩn bị tải: " + pendingMovieToDownload.getTitle(), Toast.LENGTH_SHORT).show();
            }
            // If startDownload doesn't start (e.g. already downloaded/downloading), it might show its own toast or update UI.
            pendingMovieToDownload = null; // Clear pending movie after attempting to start
        }
    }

    private boolean checkAndRequestStoragePermission() {
        // For Android 10 (API 29) and above, direct WRITE_EXTERNAL_STORAGE is less relevant
        // if you are saving to app-specific directories. MediaStore API is preferred for shared media.
        // This example keeps the M-P logic for WRITE_EXTERNAL_STORAGE.
        // For broader compatibility, you'd need to handle Scoped Storage for API 29+
        // and potentially READ_MEDIA_VIDEO for API 33+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // Android 6 to 9
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_DOWNLOAD_PERMISSION);
                return false; // Permission not yet granted, will be handled in onRequestPermissionsResult
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
                    boolean downloadWillActuallyStart = downloadViewModel.startDownload(pendingMovieToDownload);
                    if (downloadWillActuallyStart) {
                        Toast.makeText(this, "Đang chuẩn bị tải: " + pendingMovieToDownload.getTitle(), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Quyền lưu trữ bị từ chối. Không thể tải phim.", Toast.LENGTH_LONG).show();
            }
            pendingMovieToDownload = null; // Clear pending movie after permission result
        }
    }

    @Override
    public void onEpisodeClick(MovieDetailResponse.Episode episode) {
        Toast.makeText(this, "Chuyển sang: " + episode.getDescription(), Toast.LENGTH_SHORT).show();
        if (episode.getLink() != null && !episode.getLink().isEmpty()) {
            episodeUrl = episode.getLink(); // Cập nhật URL
            currentPlayingEpisodeSlug = episode.getSlug(); // Cập nhật slug tập đang phát
            playVideo(); // Phát video mới

            // Cập nhật trạng thái highlight trong adapter
            episodeAdapter.setCurrentPlayingEpisode(currentPlayingEpisodeSlug);
        } else {
            Toast.makeText(this, "Link tập phim không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }
}