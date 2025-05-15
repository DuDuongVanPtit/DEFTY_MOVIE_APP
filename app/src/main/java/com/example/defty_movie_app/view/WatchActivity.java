package com.example.defty_movie_app.view;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.core.widget.NestedScrollView; // Đảm bảo dùng NestedScrollView
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

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
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // Cho Download
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector.SelectionOverride;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.CastCrewAdapter;
import com.example.defty_movie_app.adapter.CommentAdapter;
import com.example.defty_movie_app.adapter.EpisodeAdapter;
import com.example.defty_movie_app.adapter.RecommendedMovieAdapter;
import com.example.defty_movie_app.config.AppConstants;
import com.example.defty_movie_app.data.dto.DownloadedMovie;
import com.example.defty_movie_app.data.dto.Movie;
import com.example.defty_movie_app.data.model.adapter.CastCrew;
import com.example.defty_movie_app.data.model.request.MovieCommentRequest; // Đổi tên package nếu cần
import com.example.defty_movie_app.data.model.response.EpisodeResponse;
import com.example.defty_movie_app.data.model.response.MovieCommentResponse; // Đổi tên package nếu cần
import com.example.defty_movie_app.data.model.response.MovieDetailResponse;
import com.example.defty_movie_app.data.model.response.PageableResponse;
import com.example.defty_movie_app.data.model.response.RecommendedMovieResponse;
import com.example.defty_movie_app.data.model.response.SimpleResponse;
import com.example.defty_movie_app.data.remote.AuthApiService;
import com.example.defty_movie_app.data.remote.RecommenderServiceApi;
import com.example.defty_movie_app.data.repository.AuthRepository;
import com.example.defty_movie_app.data.repository.CallRecommender;
import com.example.defty_movie_app.utils.DownloadCompletionReceiver; // Cho Download
import com.example.defty_movie_app.utils.GridSpacingItemDecoration;
import com.example.defty_movie_app.utils.LocaleHelper;
import com.example.defty_movie_app.viewmodel.DownloadViewModel;
import com.google.android.material.tabs.TabLayout;

import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchActivity extends AppCompatActivity implements EpisodeAdapter.OnEpisodeClickListener, CommentAdapter.CommentInteractionListener, RecommendedMovieAdapter.OnMovieClickListener {

    private static final String TAG = "WatchActivity";

    // UI Elements
    private TextView textTitle, textDescription, textDescription1, btnToggleDescription;
    private ImageView coverImage;
    private ImageButton btnPlay;
    private NestedScrollView scrollContent; // Đã sửa thành NestedScrollView
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
    private TextView anchorEpisodes, anchorForYou, anchorComments;
    private boolean isTabClickScrolling = false;
    private boolean isUserScrolling = true;
    private Handler scrollSyncHandler = new Handler(Looper.getMainLooper());
    private int tabLayoutHeight = 0; // Sửa: Biến này sẽ được dùng để tính offset

    // Comments
    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private EditText editTextCommentInput;
    private ImageButton buttonSendComment;
    private ProgressBar progressBarComments;
    private TextView textNoComments;
    private View commentInputBox; // View cho toàn bộ ô nhập liệu
    private LinearLayout pageContainerScrollableContent; // Thêm biến này

    private int currentCommentPage = 0;
    private boolean isLoadingComments = false;
    private boolean isLastCommentPage = false;
    private Integer currentEpisodeIdForComments;

    // Data
    private Integer movieId;
    private String currentMovieSlug;
    private Integer episodeId; // ID của tập phim hiện tại (cho download và comment)
    private Integer episodeNumber; // Số thứ tự tập (cho download)


    // --- DOWNLOAD: Variables ---
    private ImageButton iconDownloadMovieButton;
    private DownloadViewModel downloadViewModel;
    private static final int REQUEST_CODE_DOWNLOAD_PERMISSION = 201;
    private DownloadedMovie pendingMovieToDownload;
    private String currentMovieTitle;
    private String currentCoverImageUrl;
    private BroadcastReceiver downloadStatusReceiver;
    // --- DOWNLOAD: End Variables ---
    private String processedLink;

    private Toolbar toolbarWatchActivity;

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
        setupToolbar();

        if (tabLayout != null) {
            tabLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (tabLayout.getHeight() > 0) {
                        tabLayoutHeight = tabLayout.getHeight();
                        tabLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        Log.d(TAG, "TabLayout height calculated: " + tabLayoutHeight);
                        // Gọi setupTabsAndScrollListener sau khi có tabLayoutHeight nếu cần offset chính xác ngay
                        // Hoặc đảm bảo logic trong listener sử dụng tabLayoutHeight một cách an toàn
                    }
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
        setupDownloadStatusReceiver();
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
        scrollContent = findViewById(R.id.scrollContent); // Đảm bảo ID này trỏ đến NestedScrollView

        recyclerViewEpisodes = findViewById(R.id.recyclerViewEpisodes);
        episodeRangeContainer = findViewById(R.id.episode_range_container);

        // Tabs & Anchors
        tabLayout = findViewById(R.id.tabLayout);
        scrollContent = findViewById(R.id.scrollContent);
        anchorEpisodes = findViewById(R.id.anchor_episodes);
        anchorForYou = findViewById(R.id.anchor_for_you);
        anchorComments = findViewById(R.id.anchor_comments);

        iconDownloadMovieButton = findViewById(R.id.iconDownloadMovieButton);

        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        editTextCommentInput = findViewById(R.id.commentInput);
        buttonSendComment = findViewById(R.id.sendButton);
        commentInputBox = findViewById(R.id.commentInputBox); // Ánh xạ commentInputBox
        progressBarComments = findViewById(R.id.progressBarComments);
        textNoComments = findViewById(R.id.text_no_comments);

        toolbarWatchActivity = findViewById(R.id.toolbar_watch_activity);

        // Ánh xạ LinearLayout chứa nội dung cuộn được
        pageContainerScrollableContent = findViewById(R.id.pageContainer_scrollable_content);
        if (pageContainerScrollableContent == null) {
            Log.e(TAG, "pageContainer_scrollable_content not found! Check your layout ID.");
        }
    }

    private void setupToolbar() {
        if (toolbarWatchActivity != null) {
            setSupportActionBar(toolbarWatchActivity); // Đặt Toolbar này làm ActionBar cho Activity
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút Up (mũi tên quay lại)
                getSupportActionBar().setDisplayShowTitleEnabled(false); // Ẩn tiêu đề mặc định của Toolbar (nếu bạn muốn tự quản lý tiêu đề hoặc không cần)
                // Bạn có thể set icon khác cho nút Up nếu muốn:
                // getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_your_custom_back_arrow);
            }
            // Xử lý sự kiện khi nút Up được nhấn
            toolbarWatchActivity.setNavigationOnClickListener(v -> onBackPressed()); // Hoặc finish();
        } else {
            Log.e(TAG, "Toolbar (toolbar_watch_activity) not found in layout!");
        }
    }

    private void setupRecyclerViews() {
        // Cast & Crew
        if (recyclerViewCastCrew != null) {
            recyclerViewCastCrew.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerViewCastCrew.setHasFixedSize(true);
            // Adapter sẽ được set trong fetchMovieDetail
        }

        // Recommended Movies
        if (recyclerViewRecommended != null) {
            recyclerViewRecommended.setLayoutManager(new GridLayoutManager(this, 3));
            recyclerViewRecommended.setHasFixedSize(true);
            try {
                recyclerViewRecommended.addItemDecoration(new GridSpacingItemDecoration(3, 16));
            } catch (Exception e) {
                Log.w(TAG, "GridSpacingItemDecoration error.", e);
            }
            recommendedAdapter = new RecommendedMovieAdapter(new ArrayList<>(), this);
            recyclerViewRecommended.setAdapter(recommendedAdapter);
        }

        // Episodes List
        if (recyclerViewEpisodes != null) {
            recyclerViewEpisodes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerViewEpisodes.setHasFixedSize(true);
            episodeAdapter = new EpisodeAdapter(this, this);
            recyclerViewEpisodes.setAdapter(episodeAdapter);
        }

        // Setup cho recyclerViewComments
        if (recyclerViewComments != null) {
            recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewComments.setNestedScrollingEnabled(false);
            commentAdapter = new CommentAdapter(this, this);
            recyclerViewComments.setAdapter(commentAdapter);

            recyclerViewComments.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    // Chỉ xử lý nếu người dùng thực sự cuộn xuống (dy > 0)
                    if (dy > 0) {
                        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        if (layoutManager != null && commentAdapter != null && commentAdapter.getItemCount() > 0) {
                            int lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();
                            int totalItemCount = commentAdapter.getItemCount();

                            // Load more khi item cuối cùng hiển thị VÀ danh sách không rỗng
                            // Thêm một khoảng đệm nhỏ (ví dụ: 1-2 item) để tải sớm hơn một chút
                            int threshold = 1;
                            if (lastVisibleItemPosition >= totalItemCount - 1 - threshold) {
                                if (!isLoadingComments && !isLastCommentPage && currentEpisodeIdForComments != null) {
                                    Log.d(TAG, "SCROLL LISTENER: Load more comments for episode: " + currentEpisodeIdForComments + ", page: " + (currentCommentPage + 1));
                                    fetchCommentsForEpisode(currentEpisodeIdForComments, currentCommentPage + 1, false);
                                }
                            }
                        }
                    }
                }
            });
        }
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
        downloadViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);
        downloadViewModel.getDownloadedMoviesLiveData().observe(this, new Observer<List<DownloadedMovie>>() {
            @Override
            public void onChanged(List<DownloadedMovie> downloadedMovies) {
                Log.d(TAG, "Downloaded movies list changed in LiveData, updating button state.");
                // Gọi updateDownloadButtonState() ở đây là đúng vì LiveData đã thay đổi
                // Nó sẽ được trigger bởi loadDownloadedMovies() từ broadcast receiver
                updateDownloadButtonState();
            }
        });
        // Không cần gọi loadDownloadedMovies() ở đây nữa nếu onResume đã gọi
    }

    private void setupDownloadStatusReceiver() {
        downloadStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received ACTION_DOWNLOAD_STATUS_CHANGED broadcast");
                // long downloadId = intent.getLongExtra(DownloadCompletionReceiver.EXTRA_DOWNLOAD_ID, -1);
                // Log.d(TAG, "Download ID from broadcast: " + downloadId);

                // Yêu cầu ViewModel tải lại danh sách từ SharedPreferences
                // Điều này sẽ kích hoạt LiveData observer và cập nhật UI
                if (downloadViewModel != null) {
                    Log.d(TAG, "Telling DownloadViewModel to reload movies.");
                    downloadViewModel.loadDownloadedMovies();
                }
            }
        };
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

        // --- KIỂM TRA ĐĂNG NHẬP KHI NHẤN VÀO Ô NHẬP BÌNH LUẬN ---
        if (editTextCommentInput != null) {
            editTextCommentInput.setOnClickListener(v -> checkLoginAndFocusComment());
            editTextCommentInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    checkLoginAndFocusComment();
                }
            });
        }
        // --- ---

        if (buttonSendComment != null) {
            buttonSendComment.setOnClickListener(v -> postNewCommentWithLoginCheck());
        }

        if (scrollContent != null && anchorComments != null && commentInputBox != null && recyclerViewComments != null) {

            scrollContent.getViewTreeObserver().addOnScrollChangedListener(() -> {

                if (anchorComments == null || scrollContent == null || commentInputBox == null || recyclerViewComments == null) return;

                int scrollY = scrollContent.getScrollY();

                int scrollViewHeight = scrollContent.getHeight();
                int anchorCommentsTop = anchorComments.getTop();
                if (anchorCommentsTop < (scrollY + scrollViewHeight) && (anchorCommentsTop + anchorComments.getHeight()) > scrollY && commentAdapter != null && commentAdapter.getItemCount() > 0) {
                    if (tabLayout != null && tabLayout.getSelectedTabPosition() == 2) {
                        commentInputBox.setVisibility(View.VISIBLE);
                    } else {
                        commentInputBox.setVisibility(View.GONE);
                    }
                } else {
                    commentInputBox.setVisibility(View.GONE);
                }
            });

        }
    }

    // Implement các phương thức của CommentInteractionListener
    @Override
    public void onLikeClicked(MovieCommentResponse comment, int position) {
        Toast.makeText(this, "Đã thích bình luận: " + comment.getContent().substring(0, Math.min(comment.getContent().length(), 10)) + "...", Toast.LENGTH_SHORT).show();
        // TODO: Gọi API để like/unlike comment và cập nhật UI
    }

    @Override
    public void onReplyClicked(MovieCommentResponse comment, int position) {
        Toast.makeText(this, "Trả lời bình luận của: " + comment.getUserName(), Toast.LENGTH_SHORT).show();
        // TODO: Hiển thị UI để nhập nội dung trả lời, có thể set parentCommentId
        // Ví dụ: editTextCommentInput.setHint("Trả lời " + comment.getUserName() + "...");
        // editTextCommentInput.requestFocus();
        // InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        // imm.showSoftInput(editTextCommentInput, InputMethodManager.SHOW_IMPLICIT);
        // Lưu parentCommentId để khi postNewComment sẽ gửi kèm.
    }

    @Override
    public void onViewRepliesClicked(MovieCommentResponse parentComment, int position) {
        if (parentComment == null || parentComment.getId() == null) {
            Toast.makeText(this, "Không thể tải trả lời, ID bình luận không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiển thị một dialog loading tạm thời (tùy chọn)
        // AlertDialog loadingDialog = new AlertDialog.Builder(this)
        //         .setMessage("Đang tải trả lời...")
        //         .setCancelable(false)
        //         .show();

        Log.d(TAG, "Fetching replies for comment ID: " + parentComment.getId());
        Toast.makeText(this, "Đang tải trả lời cho bình luận của: " + parentComment.getUserName(), Toast.LENGTH_SHORT).show();


        AuthApiService apiService = AuthRepository.getInstance().getApi();
        apiService.getMovieCommentReplies(parentComment.getId())
                .enqueue(new Callback<SimpleResponse<List<MovieCommentResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<SimpleResponse<List<MovieCommentResponse>>> call,
                                           @NonNull Response<SimpleResponse<List<MovieCommentResponse>>> response) {
                        // if (loadingDialog != null && loadingDialog.isShowing()) {
                        //     loadingDialog.dismiss();
                        // }

                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            List<MovieCommentResponse> replies = response.body().getData();
                            if (replies.isEmpty()) {
                                Toast.makeText(WatchActivity.this, "Không có trả lời nào cho bình luận này.", Toast.LENGTH_SHORT).show();
                            } else {
                                showRepliesDialog(parentComment, replies);
                            }
                        } else {
                            Log.e(TAG, "Error fetching replies: " + response.code() + " - " + response.message());
                            Toast.makeText(WatchActivity.this, "Lỗi khi tải trả lời: " + response.message(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SimpleResponse<List<MovieCommentResponse>>> call, @NonNull Throwable t) {
                        // if (loadingDialog != null && loadingDialog.isShowing()) {
                        //     loadingDialog.dismiss();
                        // }
                        Log.e(TAG, "Failure fetching replies", t);
                        Toast.makeText(WatchActivity.this, "Lỗi mạng khi tải trả lời.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Hàm mới để hiển thị replies trong một AlertDialog
    private void showRepliesDialog(MovieCommentResponse parentComment, List<MovieCommentResponse> replies) {
        if (replies == null || replies.isEmpty()) {
            return;
        }

        // Tạo một mảng CharSequence để hiển thị trong dialog
        CharSequence[] items = new CharSequence[replies.size()];
        for (int i = 0; i < replies.size(); i++) {
            MovieCommentResponse reply = replies.get(i);
            String replyText = "";
            if (reply.getUser() != null && reply.getUser().getFullName() != null) {
                replyText += reply.getUser().getFullName();
            } else {
                replyText += "Người dùng ẩn danh";
            }
            replyText += ": " + reply.getContent();
            // Bạn có thể thêm thông tin thời gian nếu muốn
            // replyText += "\n(" + commentAdapter.formatTimestamp(reply.getCreatedAt(), this) + ")"; // Cần commentAdapter hoặc hàm formatTimestamp ở đây
            items[i] = replyText;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom); // Sử dụng style tùy chỉnh của bạn
        builder.setTitle("Trả lời cho: " + parentComment.getUserName());
        builder.setItems(items, null); // null listener vì chỉ hiển thị, không có hành động khi click item
        builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    // --- HÀM MỚI ĐỂ ĐIỀU CHỈNH PADDING CHO NỘI DUNG CUỘN ---
    private void adjustScrollableContentPadding() {
        if (pageContainerScrollableContent == null || commentInputBox == null) {
            Log.w(TAG, "adjustScrollableContentPadding: pageContainer or commentInputBox is null.");
            return;
        }

        if (commentInputBox.getVisibility() == View.VISIBLE) {
            if (commentInputBox.getHeight() == 0) {
                // Nếu commentInputBox chưa được đo, post một runnable để thực hiện sau khi layout pass
                commentInputBox.post(() -> {
                    if (commentInputBox.getHeight() > 0) {
                        Log.d(TAG, "Adjusting padding. CommentInputBox height: " + commentInputBox.getHeight());
                        pageContainerScrollableContent.setPadding(
                                pageContainerScrollableContent.getPaddingLeft(),
                                pageContainerScrollableContent.getPaddingTop(),
                                pageContainerScrollableContent.getPaddingRight(),
                                commentInputBox.getHeight() // Thêm padding bằng chiều cao của ô nhập
                        );
                    }
                });
            } else {
                Log.d(TAG, "Adjusting padding. CommentInputBox height: " + commentInputBox.getHeight());
                pageContainerScrollableContent.setPadding(
                        pageContainerScrollableContent.getPaddingLeft(),
                        pageContainerScrollableContent.getPaddingTop(),
                        pageContainerScrollableContent.getPaddingRight(),
                        commentInputBox.getHeight()
                );
            }
        } else {
            Log.d(TAG, "Adjusting padding. CommentInputBox is GONE. Setting bottom padding to 0.");
            pageContainerScrollableContent.setPadding(
                    pageContainerScrollableContent.getPaddingLeft(),
                    pageContainerScrollableContent.getPaddingTop(),
                    pageContainerScrollableContent.getPaddingRight(),
                    0 // Xóa padding khi ô nhập ẩn
            );
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
                if (isTabClickScrolling) return;
                View targetView = null;
                switch (tab.getPosition()) {
                    case 0: targetView = anchorEpisodes; break;
                    case 1: targetView = anchorForYou; break;
                    case 2: targetView = anchorComments; break;
                }
                if (targetView != null) {
                    final View finalTargetView = targetView;
                    finalTargetView.post(() -> {
                        int targetY = finalTargetView.getTop();
                        int paddingTopToAvoidTab = (int) (16 * getResources().getDisplayMetrics().density); // Khoảng đệm nhỏ
                        isTabClickScrolling = true;
                        scrollContent.smoothScrollTo(0, targetY - paddingTopToAvoidTab);
                        scrollSyncHandler.removeCallbacksAndMessages(null);
                        scrollSyncHandler.postDelayed(() -> isTabClickScrolling = false, 350);
                    });
                }

                // --- HIỂN THỊ/ẨN COMMENT INPUT BOX KHI CHỌN TAB VÀ ĐIỀU CHỈNH PADDING ---
                if (commentInputBox != null) {
                    if (tab.getPosition() == 2 && isUserLoggedIn()) { // Tab "Bình luận" (index 2)
                        commentInputBox.setVisibility(View.VISIBLE);
                    } else {
                        commentInputBox.setVisibility(View.GONE);
                        hideKeyboard();
                    }
                    adjustScrollableContentPadding(); // Gọi sau khi thay đổi visibility
                }
                // --- ---
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) { onTabSelected(tab); }
        });

        scrollContent.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (isTabClickScrolling || tabLayout == null || anchorEpisodes == null || anchorForYou == null || anchorComments == null) {
                return;
            }

            int scrollY = scrollContent.getScrollY();
            int contentHeight = scrollContent.getChildAt(0).getHeight();
            int scrollViewHeight = scrollContent.getHeight();
            int activeThreshold = (int) (16 * getResources().getDisplayMetrics().density);

            int episodesTop = anchorEpisodes.getTop();
            int forYouTop = anchorForYou.getTop();
            int commentsTop = anchorComments.getTop();

            int newSelectedTab = -1;

            if (scrollY + activeThreshold >= commentsTop) {
                newSelectedTab = 2; // Comments
            } else if (scrollY + activeThreshold >= forYouTop) {
                newSelectedTab = 1; // For You
            } else if (scrollY + activeThreshold >= episodesTop) {
                newSelectedTab = 0; // Episodes
            }

            int currentSelectedTab = tabLayout.getSelectedTabPosition();
            if (newSelectedTab != -1 && newSelectedTab != currentSelectedTab) {
                TabLayout.Tab tabToSelect = tabLayout.getTabAt(newSelectedTab);
                if (tabToSelect != null) {
                    tabToSelect.select();
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
        LocalBroadcastManager.getInstance(this).registerReceiver(
                downloadStatusReceiver,
                new IntentFilter(DownloadCompletionReceiver.ACTION_DOWNLOAD_STATUS_CHANGED));
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadStatusReceiver);
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

                    // --- SỬ DỤNG HELPER METHOD ĐỂ XÂY DỰNG URL ---
                    episodeUrl = constructEpisodePlaybackUrl(episode.getLink());
                    processedLink = constructEpisodeProcessedUrl(episode.getProcessedLink());
                    // --- KẾT THÚC SỬ DỤNG HELPER METHOD ---

                    episodeId=episode.getId();
                    episodeNumber=episode.getNumber();

                    if (episode.getSlug() != null) {
                        currentPlayingEpisodeSlug = episode.getSlug();
                        Log.d(TAG, "Initial playing episode slug: " + currentPlayingEpisodeSlug);
                        if (episodeAdapter != null && episodeAdapter.getItemCount() > 0) {
                            episodeAdapter.setCurrentPlayingEpisode(currentPlayingEpisodeSlug);
                        }
                    }
                    Log.d(TAG, "Episode URL fetched: " + episodeUrl);
                    if (btnPlay != null) btnPlay.setEnabled(true);

                    // --- TẢI BÌNH LUẬN CHO TẬP ĐẦU TIÊN ---
                    if (episode.getId() != null) { // Giả sử EpisodeResponse.Episode có getId()
                        loadCommentsForEpisode(episode.getId(), true);
                    }
                    // --- ---
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

    private String constructEpisodePlaybackUrl(String rawLink) {
        if (rawLink == null || rawLink.isEmpty()) {
            Log.w(TAG, "Raw link for playback URL is null or empty.");
            return null;
        }
        if (rawLink.startsWith("http://") || rawLink.startsWith("https://")) {
            Log.d(TAG, "Raw link is already a full URL: " + rawLink);
            return rawLink;
        }
        String tmp = AppConstants.DOMAIN + ":8080/videos/";
        return tmp + rawLink + "/master.m3u8";
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


    // --- Các hàm xử lý Bình luận ---

    /**
     * Gọi khi một tập phim mới được chọn hoặc khi load màn hình lần đầu.
     * @param episodeId ID của tập phim để tải bình luận.
     * @param resetExisting true nếu muốn xóa các bình luận cũ và tải lại từ trang đầu.
     */
    private void loadCommentsForEpisode(int episodeId, boolean resetExisting) {
        Log.d(TAG, "Loading comments for episode ID: " + episodeId + ", reset: " + resetExisting);
        currentEpisodeIdForComments = episodeId;
        if (resetExisting) {
            currentCommentPage = 0;
            isLastCommentPage = false;
            if (commentAdapter != null) {
                commentAdapter.setComments(new ArrayList<>()); // Xóa bình luận cũ
            }
        }
        fetchCommentsForEpisode(episodeId, currentCommentPage, resetExisting);
    }


    private void fetchCommentsForEpisode(int episodeId, int pageToFetch, boolean isRefreshing) {
        // Kiểm tra nếu đang có một yêu cầu tải bình luận khác diễn ra
        if (isLoadingComments && !isRefreshing) { // Cho phép refresh ngay cả khi đang loading trang cũ
            Log.d(TAG, "fetchCommentsForEpisode: Already loading comments, request ignored for page " + pageToFetch);
            return;
        }
        isLoadingComments = true;

        // Hiển thị ProgressBar và ẩn TextView "không có bình luận" khi bắt đầu tải mới hoặc refresh
        if (progressBarComments != null && (isRefreshing || pageToFetch == 0)) {
            progressBarComments.setVisibility(View.VISIBLE);
        }
        if (textNoComments != null && (isRefreshing || pageToFetch == 0)) {
            textNoComments.setVisibility(View.GONE);
        }

        Log.d(TAG, "Fetching comments for episode: " + episodeId + ", page: " + pageToFetch);

        // Lấy instance của AuthApiService
        AuthApiService apiService = AuthRepository.getInstance().getApi(); // Đảm bảo AuthRepository đã được khởi tạo

        // Gọi API
        apiService.getMovieComments(episodeId, pageToFetch, 10) // Ví dụ: mỗi trang lấy 10 bình luận
                .enqueue(new Callback<SimpleResponse<PageableResponse<MovieCommentResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<SimpleResponse<PageableResponse<MovieCommentResponse>>> call,
                                           @NonNull Response<SimpleResponse<PageableResponse<MovieCommentResponse>>> response) {
                        isLoadingComments = false;
                        if (progressBarComments != null) {
                            progressBarComments.setVisibility(View.GONE);
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            SimpleResponse<PageableResponse<MovieCommentResponse>> simpleResponse = response.body();
                            if (simpleResponse.getData() != null) {
                                PageableResponse<MovieCommentResponse> pageableData = simpleResponse.getData();
                                List<MovieCommentResponse> fetchedComments = pageableData.getContent();

                                if (commentAdapter != null) {
                                    if (isRefreshing) {
                                        commentAdapter.setComments(fetchedComments); // Thay thế danh sách cũ
                                    } else if (fetchedComments != null && !fetchedComments.isEmpty()) {
                                        commentAdapter.addComments(fetchedComments); // Thêm vào danh sách hiện tại
                                    }
                                }

                                currentCommentPage = pageableData.getNumber(); // Cập nhật số trang hiện tại (từ 0)
                                isLastCommentPage = pageableData.isLast();     // Cập nhật cờ trang cuối

                                // Kiểm tra và hiển thị "không có bình luận" nếu danh sách rỗng
                                if (commentAdapter != null && commentAdapter.getItemCount() == 0) {
                                    if (textNoComments != null) {
                                        textNoComments.setText("Chưa có bình luận nào cho tập này.");
                                        textNoComments.setVisibility(View.VISIBLE);
                                    }
                                    Log.d(TAG, "No comments found for episode " + episodeId);
                                } else {
                                    if (textNoComments != null) {
                                        textNoComments.setVisibility(View.GONE);
                                    }
                                }
                                Log.d(TAG, "Comments loaded. Page: " + currentCommentPage + ", IsLast: " + isLastCommentPage + ", Count: " + (fetchedComments != null ? fetchedComments.size() : 0));

                            } else {
                                // Trường "data" trong SimpleResponse là null
                                Log.e(TAG, "fetchCommentsForEpisode: 'data' field in SimpleResponse is null. Response status: " + simpleResponse.getStatus() + ", message: " + simpleResponse.getMessage());
                                if (commentAdapter != null && commentAdapter.getItemCount() == 0 && textNoComments != null) {
//                                    textNoComments.setText("Lỗi tải bình luận (dữ liệu rỗng).");
                                    textNoComments.setVisibility(View.VISIBLE);
                                }
//                                Toast.makeText(WatchActivity.this, "Lỗi tải bình luận (dữ liệu không hợp lệ).", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Phản hồi API không thành công (ví dụ: lỗi 4xx, 5xx)
                            Log.e(TAG, "fetchCommentsForEpisode: Unsuccessful response. Code: " + response.code() + ", Message: " + response.message());
                            if (commentAdapter != null && commentAdapter.getItemCount() == 0 && textNoComments != null) {
                                textNoComments.setText("Lỗi tải bình luận (mã " + response.code() + ").");
                                textNoComments.setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(WatchActivity.this, "Lỗi tải bình luận: " + response.message(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SimpleResponse<PageableResponse<MovieCommentResponse>>> call, @NonNull Throwable t) {
                        isLoadingComments = false;
                        if (progressBarComments != null) {
                            progressBarComments.setVisibility(View.GONE);
                        }
                        if (commentAdapter != null && commentAdapter.getItemCount() == 0 && textNoComments != null) {
                            textNoComments.setText("Lỗi mạng khi tải bình luận.");
                            textNoComments.setVisibility(View.VISIBLE);
                        }
                        Log.e(TAG, "fetchCommentsForEpisode: API call failed.", t);
                        Toast.makeText(WatchActivity.this, "Lỗi mạng khi tải bình luận.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // --- HÀM KIỂM TRA ĐĂNG NHẬP VÀ XỬ LÝ ---
    private boolean isUserLoggedIn() {
        // Đây là ví dụ, bạn cần thay thế bằng logic kiểm tra đăng nhập thực tế của bạn
        // Ví dụ: Kiểm tra SharedPreferences, một Singleton quản lý session, hoặc ViewModel
        SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE); // "UserData" là tên file prefs ví dụ
        return prefs.getBoolean("isLoggedIn", true); // "isLoggedIn" là key ví dụ
    }

    private void showLoginPromptDialog() {
        new AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setTitle("Yêu cầu đăng nhập")
                .setMessage("Bạn cần đăng nhập để sử dụng tính năng bình luận.")
                .setPositiveButton("Đăng nhập", (dialog, which) -> {
                    // Chuyển đến LoginActivity
//                    Intent intent = new Intent(WatchActivity.this, LoginActivity.class); // Thay LoginActivity bằng Activity đăng nhập của bạn
                    // Có thể thêm cờ để LoginActivity biết quay lại màn này sau khi đăng nhập thành công
                    // intent.putExtra("redirect_to_watch_activity_slug", currentMovieSlug);
//                    startActivity(intent);
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void checkLoginAndFocusComment() {
        if (!isUserLoggedIn()) {
            showLoginPromptDialog();
            if (editTextCommentInput != null) {
                editTextCommentInput.clearFocus(); // Bỏ focus nếu chưa đăng nhập
                hideKeyboard();
            }
        } else {
            // Đã đăng nhập, cho phép focus và hiện bàn phím
            if (editTextCommentInput != null) {
                editTextCommentInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(editTextCommentInput, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }
    }

    private void postNewCommentWithLoginCheck() {
        if (!isUserLoggedIn()) {
            showLoginPromptDialog();
            return;
        }
        // Nếu đã đăng nhập, tiến hành đăng bình luận
        postNewComment();
    }
    // --- KẾT THÚC HÀM KIỂM TRA ĐĂNG NHẬP ---

    private void postNewComment() {
        if (editTextCommentInput == null) return;
        String commentContent = editTextCommentInput.getText().toString().trim();
        if (TextUtils.isEmpty(commentContent)) {
            Toast.makeText(this, "Vui lòng nhập nội dung bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEpisodeIdForComments == null) {
            Toast.makeText(this, "Không xác định được tập phim để bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Lấy userId từ SharedPreferences hoặc nơi bạn lưu thông tin người dùng đã đăng nhập
        // Integer currentUserId = ... ;
        // if (currentUserId == null) {
        //     Toast.makeText(this, "Vui lòng đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
        //     return;
        // }

        MovieCommentRequest request = new MovieCommentRequest(currentEpisodeIdForComments, commentContent);
        // Nếu là trả lời bình luận, bạn cần thêm parentCommentId:
        // MovieCommentRequest request = new MovieCommentRequest(currentEpisodeIdForComments, commentContent, parentId);


        // Hiển thị loading hoặc vô hiệu hóa nút gửi
        if (buttonSendComment != null) buttonSendComment.setEnabled(false);
        Toast.makeText(this, "Đang gửi bình luận...", Toast.LENGTH_SHORT).show();

        AuthApiService apiService = AuthRepository.getInstance().getApi();
        // Giả sử bạn có SimpleResponse hoặc một DTO cụ thể cho kết quả post comment
        apiService.addMovieComment(request /*, "Bearer your_auth_token" nếu cần */)
                .enqueue(new Callback<SimpleResponse>() { // Hoặc Call<YourPostCommentResponse>
                    @Override
                    public void onResponse(@NonNull Call<SimpleResponse> call, @NonNull Response<SimpleResponse> response) {
                        if (buttonSendComment != null) buttonSendComment.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            // Giả sử response.body() chứa thông tin bình luận vừa tạo hoặc ID
                            Toast.makeText(WatchActivity.this, "Đăng bình luận thành công!", Toast.LENGTH_SHORT).show();
                            editTextCommentInput.setText(""); // Xóa nội dung đã nhập
                            hideKeyboard();

                            // Tải lại bình luận hoặc thêm bình luận mới vào đầu danh sách
                            // Tùy chọn 1: Tải lại toàn bộ trang đầu
                            loadCommentsForEpisode(currentEpisodeIdForComments, true);

                            // Tùy chọn 2: Nếu API trả về bình luận vừa tạo, thêm nó vào đầu adapter
                            // MovieCommentResponse newComment = parseCommentFromResponse(response.body());
                            // if (newComment != null && commentAdapter != null) {
                            //     commentAdapter.addCommentToTop(newComment);
                            //     if (recyclerViewComments != null) recyclerViewComments.scrollToPosition(0);
                            // }

                        } else {
                            Log.e(TAG, "postComment: Error posting comment, code: " + response.code() + ", message: " + response.message());
                            Toast.makeText(WatchActivity.this, "Lỗi: Không thể đăng bình luận. " + response.message(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SimpleResponse> call, @NonNull Throwable t) {
                        if (buttonSendComment != null) buttonSendComment.setEnabled(true);
                        Log.e(TAG, "postComment: API call failed", t);
                        Toast.makeText(WatchActivity.this, "Lỗi mạng: Không thể đăng bình luận.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }


    // --- DOWNLOAD: Methods ---
    private void updateDownloadButtonState() {
        if (iconDownloadMovieButton == null || episodeId == null || TextUtils.isEmpty(currentPlayingEpisodeSlug)) {
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
                if (downloadedMovie.getId() == episodeId && currentPlayingEpisodeSlug.equals(downloadedMovie.getSlug())) {
                    currentMovieInList = downloadedMovie;
                    break;
                }
            }
        }

        if (currentMovieInList != null) {
            String status = currentMovieInList.getDownloadStatus();
            Log.d(TAG, "updateDownloadButtonState for episode " + episodeId + " ("+ currentPlayingEpisodeSlug +"): Status = " + status);

            iconDownloadMovieButton.setImageResource(R.drawable.ic_download); // Luôn dùng icon download gốc

            if (DownloadedMovie.STATUS_COMPLETED.equals(status)) {
                iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_completed_green), PorterDuff.Mode.SRC_IN);
            } else if (DownloadedMovie.STATUS_DOWNLOADING.equals(status) || DownloadedMovie.STATUS_PENDING.equals(status)) {
                // Khi đang tải hoặc chờ tải, đổi sang màu xanh dương
                iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_downloading_blue), PorterDuff.Mode.SRC_IN);
            } else if (DownloadedMovie.STATUS_FAILED.equals(status)) {
                iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_failed_red), PorterDuff.Mode.SRC_IN);
            }
            else { // Các trường hợp khác hoặc trạng thái không xác định (coi như mặc định)
                iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_default_tint), PorterDuff.Mode.SRC_IN);
            }
        } else { // Không có trong danh sách tải xuống
            Log.d(TAG, "updateDownloadButtonState for episode " + episodeId + " ("+ currentPlayingEpisodeSlug +"): Not in download list.");
            iconDownloadMovieButton.setImageResource(R.drawable.ic_download);
            iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_default_tint), PorterDuff.Mode.SRC_IN);
        }
    }

    private void handleDownloadClick() {
        if (movieId == null || TextUtils.isEmpty(currentMovieTitle) ||
                TextUtils.isEmpty(currentCoverImageUrl) || TextUtils.isEmpty(processedLink) || // processedLink is important!
                TextUtils.isEmpty(currentMovieSlug)) {
            Toast.makeText(this, "Thông tin phim chưa sẵn sàng để tải. Vui lòng thử lại sau.", Toast.LENGTH_LONG).show();
            Log.d("DownloadInfo", "Thông tin còn thiếu để tải: movieId=" + movieId +
                    ", title=" + currentMovieTitle + ", cover=" + currentCoverImageUrl +
                    ", processedLink=" + processedLink + ", slug=" + currentMovieSlug);
            return;
        }

        DownloadedMovie movieToDownload = new DownloadedMovie(
                episodeId,
                currentMovieTitle,
                currentCoverImageUrl,
                processedLink,
                currentPlayingEpisodeSlug,
                episodeNumber
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
        // --- SỬ DỤNG HELPER METHOD ĐỂ XÂY DỰNG URL ---
        String newEpisodeUrl = constructEpisodePlaybackUrl(episode.getLink());
        String newProcessedLink = constructEpisodeProcessedUrl(episode.getProcessedLink());
        // --- KẾT THÚC SỬ DỤNG HELPER METHOD ---


        if (newEpisodeUrl != null) { // Chỉ tiếp tục nếu URL phát video hợp lệ
            episodeUrl = newEpisodeUrl;
            processedLink = newProcessedLink; // Cập nhật cả processedLink
            currentPlayingEpisodeSlug = episode.getSlug();
            episodeId = episode.getId();
            episodeNumber = episode.getNumber();

            playVideo();
            episodeAdapter.setCurrentPlayingEpisode(currentPlayingEpisodeSlug);
            updateDownloadButtonState();

            loadCommentsForEpisode(episode.getId(), true);
        } else {
            Toast.makeText(this, "Link tập phim không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    private String constructEpisodeProcessedUrl(String rawProcessedLink) {
        if (rawProcessedLink == null || rawProcessedLink.isEmpty()) {
            Log.w(TAG, "Raw processed link for URL is null or empty.");
            return null;
        }
        if (rawProcessedLink.startsWith("http://") || rawProcessedLink.startsWith("https://")) {
            Log.d(TAG, "Raw processed link is already a full URL: " + rawProcessedLink);
            return rawProcessedLink;
        }
        String tmp = AppConstants.DOMAIN + ":8080/videos/";
        return tmp + rawProcessedLink;
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
    @Override
    public void onMovieClick(RecommendedMovieResponse.RecommendedMovie movie) {
        Context context = WatchActivity.this;
        if (context != null && movie != null && movie.slug != null && !movie.slug.isEmpty()) {
            Log.d(TAG, "onMovieClick: Movie clicked with slug: " + movie.slug);
            // Start WatchActivity directly with the banner's content slug
            Intent intent = new Intent(context, WatchActivity.class);
            intent.putExtra("MOVIE_SLUG_ID", movie.slug); // Use a consistent key
            context.startActivity(intent);
        } else {
            Log.w(TAG, "onBannerClick: Cannot start WatchActivity. Context null or banner/slug empty.");
            if (context != null) {
                Toast.makeText(context, "Cannot open banner content.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}