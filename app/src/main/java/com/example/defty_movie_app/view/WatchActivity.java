package com.example.defty_movie_app.view;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler; // Thêm import
import android.os.Looper;  // Thêm import
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver; // Thêm import
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
//import android.widget.ScrollView;
import androidx.core.widget.NestedScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.CastCrewAdapter;
import com.example.defty_movie_app.adapter.EpisodeAdapter;
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
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.TrackGroupArray; // Quan trọng: Import đúng TrackGroupArray
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.ui.PlayerView;

public class WatchActivity extends AppCompatActivity implements EpisodeAdapter.OnEpisodeClickListener {

    private static final String TAG = "WatchActivity";

    // UI Elements
    private TextView textTitle, textDescription, textDescription1, btnToggleDescription;
    private ImageView coverImage;
    private ImageButton btnPlay;
//    private ScrollView scrollContent;
    private androidx.core.widget.NestedScrollView scrollContent;
    private RecyclerView recyclerViewCastCrew;
    private ProgressBar progressBarRecommended;
    private RecyclerView recyclerViewRecommended;
    private RecommendedMovieAdapter recommendedAdapter;
    private PlayerView playerView;
    private ImageButton btnPlayCustom, btnPauseCustom;

    // Player
    private ExoPlayer player;
    private String episodeUrl;
    private ActivityResultLauncher<Intent> fullscreenLauncher;
    private long startPositionToResume = 0;
    private Player.Listener playerListener;

    // Episode List & Ranges
    private RecyclerView recyclerViewEpisodes;
    private EpisodeAdapter episodeAdapter;
    private String currentPlayingEpisodeSlug;
    private List<MovieDetailResponse.Episode> allEpisodesList = new ArrayList<>();
    private int currentRangeStart = 0;
    private final int EPISODES_PER_RANGE = 10;
    private LinearLayout episodeRangeContainer;

    // Tabs & Anchors for ScrollSpy
    private TabLayout tabLayout;
    private TextView anchorEpisodes, anchorForYou, anchorComments; // Các View làm điểm neo
    private boolean isTabClickScrolling = false; // Cờ tránh vòng lặp scroll/tab select
    private boolean isUserScrolling = true; // Cờ kiểm soát việc scroll do người dùng hay do code
    private Handler scrollSyncHandler = new Handler(Looper.getMainLooper());
    private int tabScrollOffset = 0; // Offset để cuộn (ví dụ: chiều cao của TabLayout)


    // Comments (giữ lại nếu layout XML có, logic chưa xử lý)
    private View commentList; // Giả sử là LinearLayout tĩnh
    private View commentInputBox;

    // Data
    private Integer movieId;
    private String currentMovieSlug;

    // --- DOWNLOAD: Variables ---
    private ImageButton iconDownloadMovieButton;
    private DownloadViewModel downloadViewModel;
    private static final int REQUEST_CODE_DOWNLOAD_PERMISSION = 201;
    private DownloadedMovie pendingMovieToDownload;
    private String currentMovieTitle;
    private String currentCoverImageUrl;

    private int tabLayoutHeight = 0; // Biến lưu chiều cao của TabLayout để tính offset

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

        findViews();

        if (tabLayout != null) {
            tabLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    tabLayoutHeight = tabLayout.getHeight();
                    // Gỡ bỏ listener sau khi lấy được chiều cao để tránh gọi lại nhiều lần
                    tabLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }

        setupRecyclerViews();
        setupFullscreenLauncher();
        setupTabsAndScrollListener();

        currentMovieSlug = getIntent().getStringExtra("MOVIE_SLUG_ID");

        if (currentMovieSlug == null || currentMovieSlug.isEmpty()) {
            Log.e(TAG, "MOVIE_SLUG_ID is missing!");
            Toast.makeText(this, "Movie not found", Toast.LENGTH_LONG).show();
            if (iconDownloadMovieButton != null) iconDownloadMovieButton.setEnabled(false);
            finish();
            return;
        }

        initializeDownloadFeature();
        fetchMovieDetail(currentMovieSlug);
        fetchEpisode(currentMovieSlug);
        setupOtherListeners(); // Đổi tên từ setupListeners để phân biệt
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

        recyclerViewEpisodes = findViewById(R.id.recyclerViewEpisodes);
        episodeRangeContainer = findViewById(R.id.episode_range_container);

        // Tabs & Anchors
        tabLayout = findViewById(R.id.tabLayout);
        scrollContent = findViewById(R.id.scrollContent);
        anchorEpisodes = findViewById(R.id.anchor_episodes);
        anchorForYou = findViewById(R.id.anchor_for_you);
        anchorComments = findViewById(R.id.anchor_comments);

        commentList = findViewById(R.id.commentList);
        commentInputBox = findViewById(R.id.commentInputBox);
        iconDownloadMovieButton = findViewById(R.id.iconDownloadMovieButton);
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
    // Đặt hàm này trong class WatchActivity.java

    @OptIn(markerClass = UnstableApi.class)
    private void setupFullscreenLauncher() {
        fullscreenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        long lastPosition = result.getData().getLongExtra(FullscreenPlayerActivity.RESULT_LAST_POSITION, 0);
                        // --- BEGIN: Nhận và áp dụng cài đặt từ Fullscreen ---
                        float lastSpeed = result.getData().getFloatExtra(FullscreenPlayerActivity.RESULT_PLAYBACK_SPEED, 1.0f);
                        boolean qualityIsAuto = result.getData().getBooleanExtra(FullscreenPlayerActivity.RESULT_QUALITY_IS_AUTO, true);
                        int qualityRendererIndex = -1, qualityGroupIndex = -1, qualityTrackIndex = -1;

                        if (!qualityIsAuto) {
                            qualityRendererIndex = result.getData().getIntExtra(FullscreenPlayerActivity.RESULT_QUALITY_RENDERER_INDEX, -1);
                            qualityGroupIndex = result.getData().getIntExtra(FullscreenPlayerActivity.RESULT_QUALITY_GROUP_INDEX_IN_RENDERER_TRACK_GROUPS, -1);
                            qualityTrackIndex = result.getData().getIntExtra(FullscreenPlayerActivity.RESULT_QUALITY_TRACK_INDEX_IN_GROUP, -1);
                        }
                        Log.d(TAG, "Returned from fullscreen. Pos: " + lastPosition + ", Speed: " + lastSpeed +
                                ", QualityAuto: " + qualityIsAuto + ", R:" + qualityRendererIndex +
                                ", G:" + qualityGroupIndex + ", T:" + qualityTrackIndex);

                        startPositionToResume = lastPosition;

                        // Áp dụng cài đặt cho player của WatchActivity
                        if (player == null) {
                            initializePlayer(); // Sẽ dùng startPositionToResume
                        }

                        if (player != null) {
                            player.setPlaybackParameters(new PlaybackParameters(lastSpeed));

                            if (player.getTrackSelector() instanceof DefaultTrackSelector) {
                                DefaultTrackSelector trackSelector = (DefaultTrackSelector) player.getTrackSelector();
                                DefaultTrackSelector.Parameters.Builder paramsBuilder = trackSelector.getParameters().buildUpon();
                                MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

                                if (mappedTrackInfo != null && qualityRendererIndex != -1) { // Cần rendererIndex để áp dụng
                                    androidx.media3.exoplayer.source.TrackGroupArray rendererTrackGroups = mappedTrackInfo.getTrackGroups(qualityRendererIndex);
                                    if (qualityIsAuto) {
                                        paramsBuilder.clearSelectionOverrides(qualityRendererIndex);
                                    } else if (qualityGroupIndex != -1 && qualityTrackIndex != -1 && rendererTrackGroups != null) {
                                        DefaultTrackSelector.SelectionOverride override =
                                                new DefaultTrackSelector.SelectionOverride(qualityGroupIndex, qualityTrackIndex);
                                        paramsBuilder.setSelectionOverride(qualityRendererIndex, rendererTrackGroups, override);
                                    }
                                    trackSelector.setParameters(paramsBuilder.build());
                                } else if (qualityIsAuto && mappedTrackInfo != null) {
                                    // Nếu là Auto, cố gắng clear cho tất cả video renderers nếu không có specific index
                                    // hoặc tìm video renderer index đầu tiên
                                    for(int i=0; i < mappedTrackInfo.getRendererCount(); i++){
                                        if(mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_VIDEO){
                                            paramsBuilder.clearSelectionOverrides(i);
                                            break;
                                        }
                                    }
                                    trackSelector.setParameters(paramsBuilder.build());
                                }
                            }
                            // Logic resume player
                            if (player.getPlaybackState() == Player.STATE_IDLE) {
                                if (episodeUrl != null && !episodeUrl.isEmpty()) {
                                    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(episodeUrl));
                                    player.setMediaItem(mediaItem);
                                    player.prepare();
                                }
                            }
                            player.seekTo(startPositionToResume); // startPositionToResume đã được cập nhật
                            player.play();
                        }
                        // --- END: Nhận và áp dụng cài đặt ---
                        if (playerView != null) {
                            playerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // ... (xử lý khác của bạn) ...
                        if (player != null) {
                            player.play();
                        }
                    }
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                });
    }
    // Đặt hàm này trong class WatchActivity.java

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

    private void initializeDownloadFeature() {
        // Khởi tạo ViewModel
        downloadViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);

        // Theo dõi LiveData từ ViewModel
        downloadViewModel.getDownloadedMoviesLiveData().observe(this, new Observer<List<DownloadedMovie>>() {
            @Override
            public void onChanged(List<DownloadedMovie> downloadedMovies) {
                // Khi danh sách phim đã tải thay đổi (ví dụ: tải xong, xóa phim),
                // cập nhật lại trạng thái của nút download.
                Log.d(TAG, "Downloaded movies list changed, updating button state.");
                updateDownloadButtonState();
            }
        });

        // Có thể gọi loadDownloadedMovies lần đầu ở đây nếu bạn muốn cập nhật trạng thái nút download ngay khi Activity tạo
        // Tuy nhiên, bạn đang gọi nó trong fetchMovieDetail sau khi có movieId và currentMovieSlug,
        // và cả trong onResume, điều này cũng hợp lý.
        // Nếu gọi ở đây, đảm bảo currentMovieSlug và movieId có thể null ban đầu.
        // downloadViewModel.loadDownloadedMovies();
    }

    private void setupOtherListeners() { // Đổi tên hàm
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

    private void setupTabsAndScrollListener() {
        if (tabLayout == null || scrollContent == null || anchorEpisodes == null || anchorForYou == null || anchorComments == null) {
            Log.e(TAG, "setupTabsAndScrollListener: One or more required views for ScrollSpy are null.");
            return;
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (isTabClickScrolling) { // Nếu đang cuộn do code (từ scroll listener) thì không làm gì
                    return;
                }

                View targetView = null;
                switch (tab.getPosition()) {
                    case 0: targetView = anchorEpisodes; break;
                    case 1: targetView = anchorForYou; break;
                    case 2: targetView = anchorComments; break;
                }

                if (targetView != null) {
                    final View finalTargetView = targetView;
                    // Dùng post để đảm bảo getTop() trả về giá trị đúng
                    finalTargetView.post(() -> {
                        // Vị trí của anchor view là tương đối với cha của nó (LinearLayout trong ScrollView)
                        int targetY = finalTargetView.getTop();

                        // Thêm một chút padding từ trên xuống để tiêu đề không bị dính sát
                        int paddingTop = (int) (16 * getResources().getDisplayMetrics().density); // 16dp

                        isTabClickScrolling = true;
                        scrollContent.smoothScrollTo(0, targetY - paddingTop);

                        scrollSyncHandler.removeCallbacksAndMessages(null);
                        scrollSyncHandler.postDelayed(() -> isTabClickScrolling = false, 350); // Tăng nhẹ delay
                    });
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) { onTabSelected(tab); }
        });

        scrollContent.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (isTabClickScrolling || tabLayout == null || anchorEpisodes == null || anchorForYou == null || anchorComments == null) {
                return;
            }
            // isUserScrolling = true; // Không cần thiết nếu chỉ dựa vào isTabClickScrolling

            int scrollY = scrollContent.getScrollY();
            // activeThreshold là điểm mà khi một section chạm tới, tab của nó sẽ được active.
            // Ở đây, ta lấy điểm ngay dưới TabLayout làm mốc (top của ScrollView + một chút padding)
            int activeThreshold = (int)(16 * getResources().getDisplayMetrics().density); // Ví dụ: 16dp từ top của ScrollView

            int episodesTop = anchorEpisodes.getTop();
            int forYouTop = anchorForYou.getTop();
            int commentsTop = anchorComments.getTop();

            int currentSelectedTab = tabLayout.getSelectedTabPosition();
            int newSelectedTab = -1;

            if (scrollY + activeThreshold >= commentsTop) {
                newSelectedTab = 2; // Comments
            } else if (scrollY + activeThreshold >= forYouTop) {
                newSelectedTab = 1; // For You
            } else if (scrollY + activeThreshold >= episodesTop) {
                newSelectedTab = 0; // Episodes
            } else {
                // Nếu đang ở phần trên cùng (thông tin phim), có thể không chọn tab nào hoặc chọn tab đầu tiên
                // newSelectedTab = 0; // hoặc -1 tùy ý bạn muốn
            }

            if (newSelectedTab != -1 && newSelectedTab != currentSelectedTab) {
                TabLayout.Tab tabToSelect = tabLayout.getTabAt(newSelectedTab);
                if (tabToSelect != null) {
                    // isUserScrolling = false; // Không cần thiết với logic isTabClickScrolling
                    tabToSelect.select(); // Việc này sẽ trigger onTabSelected, nhưng isTabClickScrolling sẽ chặn scroll lại
                }
            }
        });
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

    @OptIn(markerClass = UnstableApi.class)
    private void openFullscreenActivity() {
        if (player != null && episodeUrl != null && !episodeUrl.isEmpty()) {
            long currentPosition = player.getCurrentPosition();
            float currentSpeed = player.getPlaybackParameters().speed; // Lấy tốc độ hiện tại

            player.pause(); // Pause trước khi chuyển Activity

            Intent intent = new Intent(this, FullscreenPlayerActivity.class);
            intent.putExtra(FullscreenPlayerActivity.EXTRA_VIDEO_URL, episodeUrl);
            intent.putExtra(FullscreenPlayerActivity.EXTRA_START_POSITION, currentPosition);
            intent.putExtra(FullscreenPlayerActivity.EXTRA_PLAYBACK_SPEED, currentSpeed); // Gửi tốc độ

            // --- BEGIN: Gửi cài đặt chất lượng hiện tại ---
            if (player.getTrackSelector() instanceof DefaultTrackSelector) {
                DefaultTrackSelector trackSelector = (DefaultTrackSelector) player.getTrackSelector();
                DefaultTrackSelector.Parameters currentParams = trackSelector.getParameters();
                MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                boolean isAuto = true;
                int rendererIndex = -1, groupIndex = -1, trackIndex = -1;

                if (mappedTrackInfo != null) {
                    for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                        if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_VIDEO) {
                            rendererIndex = i;
                            androidx.media3.exoplayer.source.TrackGroupArray rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
                            if (currentParams.hasSelectionOverride(rendererIndex, rendererTrackGroups)) {
                                DefaultTrackSelector.SelectionOverride override = currentParams.getSelectionOverride(rendererIndex, rendererTrackGroups);
                                if (override != null && override.tracks.length > 0) {
                                    isAuto = false;
                                    groupIndex = override.groupIndex; // Đây là groupIndex trong rendererTrackGroups
                                    trackIndex = override.tracks[0];  // Đây là trackIndex trong group đó
                                }
                            }
                            break; // Tìm thấy video renderer
                        }
                    }
                }
                intent.putExtra(FullscreenPlayerActivity.EXTRA_QUALITY_IS_AUTO, isAuto);
                if (!isAuto) {
                    intent.putExtra(FullscreenPlayerActivity.EXTRA_QUALITY_RENDERER_INDEX, rendererIndex);
                    intent.putExtra(FullscreenPlayerActivity.EXTRA_QUALITY_GROUP_INDEX_IN_RENDERER_TRACK_GROUPS, groupIndex);
                    intent.putExtra(FullscreenPlayerActivity.EXTRA_QUALITY_TRACK_INDEX_IN_GROUP, trackIndex);
                }
                Log.d(TAG, "To Fullscreen - QualityAuto: " + isAuto + ", R: " + rendererIndex + ", G: " + groupIndex + ", T: " + trackIndex);
            }
            // --- END: Gửi cài đặt chất lượng ---

            Log.d(TAG, "Launching fullscreen. Pos: " + currentPosition + ", Speed: " + currentSpeed);
            fullscreenLauncher.launch(intent);
        } else {
            // ... (xử lý lỗi của bạn) ...
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

        final CharSequence[] settingsOptions = {"Chọn tốc độ phát", "Chọn chất lượng video"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        builder.setTitle("Cài đặt Video");
        builder.setItems(settingsOptions, (dialog, which) -> {
            if (which == 0) {
                showSpeedSelectionDialog();
            } else if (which == 1) {
                showQualitySelectionDialog();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
    private void showSpeedSelectionDialog() {
        if (player == null) return; // Đã kiểm tra ở handleSettings nhưng thêm cho an toàn

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
        if (currentSpeedIndex == -1) currentSpeedIndex = 2; // Mặc định là 1x

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        builder.setTitle("Chọn tốc độ phát");
        builder.setSingleChoiceItems(speedOptions, currentSpeedIndex, (dialog, which) -> {
            if (player != null) {
                player.setPlaybackParameters(new PlaybackParameters(speedValues[which]));
            }
            dialog.dismiss();
            Toast.makeText(WatchActivity.this, "Tốc độ: " + speedOptions[which], Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
    @OptIn(markerClass = UnstableApi.class)
    private void showQualitySelectionDialog() {
        if (player == null || !(player.getTrackSelector() instanceof DefaultTrackSelector)) {
            Toast.makeText(this, "Không thể thay đổi chất lượng (player hoặc trackSelector không hợp lệ).", Toast.LENGTH_SHORT).show();
            return;
        }

        DefaultTrackSelector trackSelector = (DefaultTrackSelector) player.getTrackSelector();
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

        if (mappedTrackInfo == null) {
            Toast.makeText(this, "Thông tin track không có sẵn.", Toast.LENGTH_SHORT).show();
            return;
        }

        int videoRendererIndex = -1;
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_VIDEO) {
                videoRendererIndex = i;
                break;
            }
        }

        if (videoRendererIndex == -1) {
            Toast.makeText(this, "Không tìm thấy track video.", Toast.LENGTH_SHORT).show();
            return;
        }

        TrackGroupArray rendererTrackGroups = mappedTrackInfo.getTrackGroups(videoRendererIndex);
        if (rendererTrackGroups.isEmpty()) {
            Toast.makeText(this, "Không có lựa chọn chất lượng video (TrackGroupArray rỗng).", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tìm TrackGroup video chính (thường là cái đầu tiên có nhiều hơn 1 format)
        // và index của nó trong rendererTrackGroups
        androidx.media3.common.TrackGroup targetVideoTrackGroup = null; // common.TrackGroup
        int targetVideoTrackGroupIndexInRenderer = -1;

        for (int i = 0; i < rendererTrackGroups.length; i++) {
            androidx.media3.common.TrackGroup currentGroup = rendererTrackGroups.get(i); // Đây là common.TrackGroup
            if (currentGroup.length > 0) { // Kiểm tra có format nào không
                // Giả sử chúng ta lấy TrackGroup đầu tiên có format.
                // Bạn có thể thêm logic phức tạp hơn để chọn TrackGroup nếu cần.
                targetVideoTrackGroup = currentGroup;
                targetVideoTrackGroupIndexInRenderer = i; // Lưu index của TrackGroup này trong TrackGroupArray
                break;
            }
        }

        if (targetVideoTrackGroup == null) {
            Toast.makeText(this, "Không tìm thấy TrackGroup video phù hợp.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> qualityLabels = new ArrayList<>();
        List<Integer> trackIndicesWithinTargetGroup = new ArrayList<>(); // Index của format TRONG targetVideoTrackGroup

        qualityLabels.add("Tự động");
        trackIndicesWithinTargetGroup.add(-1); // Giá trị đặc biệt cho Auto

        for (int i = 0; i < targetVideoTrackGroup.length; i++) {
            Format format = targetVideoTrackGroup.getFormat(i);
            String label = format.height + "p";
            if (format.bitrate != Format.NO_VALUE) {
                label += " (~" + (format.bitrate / 1000) + "kbps)";
            }
            qualityLabels.add(label);
            trackIndicesWithinTargetGroup.add(i);
        }

        if (qualityLabels.size() <= 1 && qualityLabels.get(0).equals("Tự động")) {
            Toast.makeText(this, "Chỉ có một chất lượng video khả dụng.", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentSelectedDialogIndex = 0; // Mặc định là "Tự động"
        DefaultTrackSelector.Parameters currentParams = trackSelector.getParameters();

        if (currentParams.hasSelectionOverride(videoRendererIndex, rendererTrackGroups)) {
            // Lấy override kiểu exoplayer
            DefaultTrackSelector.SelectionOverride exoplayerOverride = currentParams.getSelectionOverride(videoRendererIndex, rendererTrackGroups);
            if (exoplayerOverride != null && exoplayerOverride.groupIndex == targetVideoTrackGroupIndexInRenderer && exoplayerOverride.tracks.length > 0) {
                int selectedTrackIndexInGroup = exoplayerOverride.tracks[0];
                for (int i = 1; i < trackIndicesWithinTargetGroup.size(); i++) { // Bắt đầu từ 1 để bỏ qua "Tự động"
                    if (trackIndicesWithinTargetGroup.get(i) == selectedTrackIndexInGroup) {
                        currentSelectedDialogIndex = i;
                        break;
                    }
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        builder.setTitle("Chọn chất lượng video");
        final int finalVideoRendererIndex = videoRendererIndex;
        final TrackGroupArray finalRendererTrackGroups = rendererTrackGroups; // exoplayer.source.TrackGroupArray
        final int finalTargetVideoTrackGroupIndexInRenderer = targetVideoTrackGroupIndexInRenderer;


        builder.setSingleChoiceItems(qualityLabels.toArray(new CharSequence[0]), currentSelectedDialogIndex,
                (dialog, which) -> {
                    DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.getParameters().buildUpon();
                    int selectedTrackIndexInGroupFromDialog = trackIndicesWithinTargetGroup.get(which);

                    if (selectedTrackIndexInGroupFromDialog == -1) { // "Tự động"
                        parametersBuilder.clearSelectionOverrides(finalVideoRendererIndex);
                    } else {
                        // Tạo DefaultTrackSelector.SelectionOverride (exoplayer type)
                        DefaultTrackSelector.SelectionOverride newExoPlayerOverride =
                                new DefaultTrackSelector.SelectionOverride(finalTargetVideoTrackGroupIndexInRenderer, selectedTrackIndexInGroupFromDialog);

                        parametersBuilder.setSelectionOverride(finalVideoRendererIndex,
                                finalRendererTrackGroups, // TrackGroupArray của renderer
                                newExoPlayerOverride);    // DefaultTrackSelector.SelectionOverride
                    }
                    trackSelector.setParameters(parametersBuilder.build());
                    dialog.dismiss();
                    Toast.makeText(WatchActivity.this, "Chất lượng: " + qualityLabels.get(which), Toast.LENGTH_SHORT).show();
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
                        allEpisodesList.clear();
                        allEpisodesList.addAll(movie.episode);
                        setupEpisodeRangeButtons(); // Hàm mới để tạo các nút chọn nhóm
                        displayEpisodesForRange(currentRangeStart); // Hiển thị nhóm đầu tiên (0 - 49)
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
            episodeUrl = episode.getLink();
            currentPlayingEpisodeSlug = episode.getSlug();
            playVideo();
            episodeAdapter.setCurrentPlayingEpisode(currentPlayingEpisodeSlug);
            // Tìm xem tập này thuộc về range nào và có thể cập nhật lại currentRangeStart nếu cần
            // (Phần này có thể không cần thiết nếu người dùng chỉ click trong range hiện tại)
            // updateRangeButtonHighlight(); // Đảm bảo nút range vẫn đúng
        } else {
            Toast.makeText(this, "Link tập phim không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupEpisodeRangeButtons() {
        if (episodeRangeContainer == null) {
            Log.e(TAG, "episodeRangeContainer is null in setupEpisodeRangeButtons. Ensure it's initialized in findViews().");
            return;
        }
        episodeRangeContainer.removeAllViews(); // Xóa các nút cũ (nếu có)

        if (allEpisodesList.isEmpty()) return;

        int totalEpisodes = allEpisodesList.size();
        for (int i = 0; i < totalEpisodes; i += EPISODES_PER_RANGE) {
            final int rangeStartForButton = i; // Biến final để dùng trong lambda
            int rangeEnd = Math.min(i + EPISODES_PER_RANGE - 1, totalEpisodes - 1);

            Button rangeButton = new Button(this);
            rangeButton.setText(String.format(Locale.getDefault(), "%d-%d", rangeStartForButton + 1, rangeEnd + 1));
            rangeButton.setTag(rangeStartForButton);

            // --- ĐIỀU CHỈNH KÍCH THƯỚC VÀ PADDING ---
            // Giảm kích thước chữ
            rangeButton.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12); // Thử với 13sp hoặc 12sp

            // Đặt padding nhỏ hơn
            int horizontalPaddingDp = 5; // dp (ví dụ: giảm từ 12 xuống 10)
            int verticalPaddingDp = 2;   // dp (ví dụ: giảm từ 6 xuống 4)
            float density = getResources().getDisplayMetrics().density;
            int horizontalPaddingPx = (int) (horizontalPaddingDp * density);
            int verticalPaddingPx = (int) (verticalPaddingDp * density);
            rangeButton.setPadding(horizontalPaddingPx, verticalPaddingPx, horizontalPaddingPx, verticalPaddingPx);

            // Để button có thể nhỏ hơn nữa, chúng ta có thể thử đặt minHeight/minWidth
            // Tuy nhiên, việc này có thể bị ảnh hưởng bởi style mặc định của Button.
            // Đối với Button chuẩn, bạn có thể thử đặt style không có minHeight/minWidth,
            // hoặc dùng MaterialButton sẽ dễ kiểm soát kích thước tối thiểu hơn.
            // Tạm thời, padding và textSize sẽ có tác động lớn nhất.
            // Nếu bạn muốn button thực sự nhỏ, có thể cần set minHeight cho nó.
            // rangeButton.setMinimumHeight((int)(30 * density)); // Ví dụ: Chiều cao tối thiểu 30dp


            rangeButton.setAllCaps(false);

            // Thiết lập LayoutParams và margin cho nút (giữ nguyên)
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            int marginInDp = 6; // Có thể giảm margin một chút nếu muốn các nút gần nhau hơn
            int marginInPx = (int) (marginInDp * density);
            params.setMargins(0, 0, marginInPx, 0);
            rangeButton.setLayoutParams(params);

            // Áp dụng background selector và text color selector (giữ nguyên)
            rangeButton.setBackgroundResource(R.drawable.range_button_background_selector);
            rangeButton.setTextColor(ContextCompat.getColorStateList(this, R.color.range_button_text_color_selector));
            // --- KẾT THÚC ĐIỀU CHỈNH ---

            rangeButton.setOnClickListener(v -> {
                currentRangeStart = rangeStartForButton;
                displayEpisodesForRange(rangeStartForButton);
            });
            episodeRangeContainer.addView(rangeButton);
        }
        updateRangeButtonHighlight(); // Highlight nút đầu tiên sau khi tạo xong tất cả các nút
    }

    private void displayEpisodesForRange(int rangeStart) {
        if (allEpisodesList.isEmpty() || episodeAdapter == null) {
            Log.w(TAG, "Cannot display episodes: list or adapter is null/empty.");
            return;
        }

        int end = Math.min(rangeStart + EPISODES_PER_RANGE, allEpisodesList.size());
        List<MovieDetailResponse.Episode> episodesToShow = new ArrayList<>();
        if (rangeStart < end) { // Đảm bảo subList không lỗi nếu rangeStart = end
            episodesToShow.addAll(allEpisodesList.subList(rangeStart, end));
        }

        episodeAdapter.updateEpisodes(episodesToShow);

        if (currentPlayingEpisodeSlug != null) {
            episodeAdapter.setCurrentPlayingEpisode(currentPlayingEpisodeSlug);
        }
        updateRangeButtonHighlight(); // Cập nhật highlight cho các nút chọn nhóm
    }
    private void updateRangeButtonHighlight() {
        if (episodeRangeContainer == null) {
            Log.e(TAG, "episodeRangeContainer is null in updateRangeButtonHighlight.");
            return;
        }
        for (int i = 0; i < episodeRangeContainer.getChildCount(); i++) {
            View child = episodeRangeContainer.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                Object tag = button.getTag();

                if (tag instanceof Integer) {
                    int buttonRangeStart = (Integer) tag;
                    // --- THAY ĐỔI CHÍNH Ở ĐÂY ---
                    button.setSelected(buttonRangeStart == currentRangeStart);
                    // --- KẾT THÚC THAY ĐỔI ---
                }
            }
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
                    if(btnPlay != null) btnPlay.setVisibility(View.GONE);
                    if(coverImage != null) coverImage.setVisibility(View.GONE);
                    if(playerView != null) playerView.setVisibility(View.VISIBLE);
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
}