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
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
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
import com.example.defty_movie_app.shared.UserManager;
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
import android.view.ViewGroup;

import android.view.MotionEvent;
import android.graphics.Rect;

public class WatchActivity extends AppCompatActivity implements EpisodeAdapter.OnEpisodeClickListener, CommentAdapter.CommentInteractionListener, RecommendedMovieAdapter.OnMovieClickListener {

    private static final String TAG = "WatchActivity";

    // UI Elements
    private TextView textTitle, textDescription, textDescription1, btnToggleDescription;
    private ImageView coverImage;
    private ImageButton btnPlay;
    private NestedScrollView scrollContent;
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
    private boolean isTabClickScrolling = false; // True khi người dùng click vào tab và đang có hành động cuộn tới anchor
    private Handler scrollSyncHandler = new Handler(Looper.getMainLooper());
    private int tabLayoutHeight = 0;
    private boolean mBlockScrollToAnchorForNextTabSelection = false; // Cờ để chặn cuộn tới anchor khi tab được chọn bởi scroll listener

    // Comments
    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private EditText editTextCommentInput;
    private ImageButton buttonSendComment;
    private ProgressBar progressBarComments;
    private TextView textNoComments;
    private View commentInputBox;
    private LinearLayout pageContainerScrollableContent;
    private boolean isCommentInputBoxVisible = false;

    private int currentCommentPage = 0;
    private boolean isLoadingComments = false;
    private boolean isLastCommentPage = false;
    private Integer currentEpisodeIdForComments;

    // Data
    private Integer movieId;
    private String currentMovieSlug;
    private Integer episodeId;
    private Integer episodeNumber;

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
    private String loggedInUserAvatarUrl = null;

    private MovieCommentResponse replyingToComment = null; // Lưu trữ comment đang được trả lời
    private String originalCommentInputHint;

    // === BIẾN MỚI CHO SOUNDPOOL ===
    private SoundPool soundPool;
    private int successSoundId;
    private boolean soundPoolLoaded = false;
    private ProgressBar sendingCommentProgressBar;

    private Integer membershipType;

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
        initializeSoundPool();

        if (tabLayout != null) {
            tabLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (tabLayout.getHeight() > 0) {
                        tabLayoutHeight = tabLayout.getHeight();
                        tabLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        Log.d(TAG, "TabLayout height calculated: " + tabLayoutHeight);
                    }
                }
            });
        }

        setupRecyclerViews();
        setupFullscreenLauncher();
        setupTabsAndScrollListener(); // Quan trọng: Gọi sau khi các anchor views đã được find
        setupCommentInputBoxVisibilityListener();

        currentMovieSlug = getIntent().getStringExtra("MOVIE_SLUG_ID");

        if (currentMovieSlug == null || currentMovieSlug.isEmpty()) {
            Log.e(TAG, "MOVIE_SLUG_ID is missing!");
            Toast.makeText(this, "Movie not found", Toast.LENGTH_LONG).show();
            if (iconDownloadMovieButton != null) iconDownloadMovieButton.setEnabled(false);
            finish();
            return;
        }
        if (editTextCommentInput != null) { // editTextCommentInput là R.id.commentInput
            originalCommentInputHint = editTextCommentInput.getHint().toString();
        }

        initializeDownloadFeature();
        setupDownloadStatusReceiver();
        fetchMovieDetail(currentMovieSlug);
        fetchEpisode(currentMovieSlug);
        setupOtherListeners();
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

        tabLayout = findViewById(R.id.tabLayout);
        anchorEpisodes = findViewById(R.id.anchor_episodes);
        anchorForYou = findViewById(R.id.anchor_for_you);
        anchorComments = findViewById(R.id.anchor_comments);

        iconDownloadMovieButton = findViewById(R.id.iconDownloadMovieButton);

        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        editTextCommentInput = findViewById(R.id.commentInput);
        buttonSendComment = findViewById(R.id.sendButton);
        commentInputBox = findViewById(R.id.commentInputBox);
        progressBarComments = findViewById(R.id.progressBarComments);
        textNoComments = findViewById(R.id.text_no_comments);

        toolbarWatchActivity = findViewById(R.id.toolbar_watch_activity);
        pageContainerScrollableContent = findViewById(R.id.pageContainer_scrollable_content);
        if (pageContainerScrollableContent == null) {
            Log.e(TAG, "pageContainer_scrollable_content not found! Check your layout ID.");
        }
        sendingCommentProgressBar = findViewById(R.id.sending_comment_progress_bar);
    }

    private void initializeSoundPool() {
        // Sử dụng AudioAttributes cho API 21+ (Lollipop)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION) // Hoặc USAGE_NOTIFICATION_EVENT
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1) // Phát một âm thanh tại một thời điểm
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            // Cách cũ hơn cho API < 21
            soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        }

        // Load âm thanh từ thư mục res/raw
        // Bạn cần tạo một file âm thanh (ví dụ: success_sound.mp3 hoặc success_sound.ogg)
        // và đặt nó vào thư mục res/raw (tạo thư mục raw nếu chưa có)
        // Ví dụ: R.raw.success_sound
        successSoundId = soundPool.load(this, R.raw.notification_sound, 1);

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool sp, int sampleId, int status) {
                if (status == 0) {
                    // Load thành công
                    soundPoolLoaded = true;
                    Log.d(TAG, "SoundPool loaded successfully. Sound ID: " + sampleId);
                } else {
                    // Load thất bại
                    soundPoolLoaded = false;
                    Log.e(TAG, "SoundPool load failed. Status: " + status);
                }
            }
        });
    }

    private void playSuccessSound() {
        if (soundPoolLoaded && soundPool != null) {
            // Phát âm thanh
            // Tham số: soundID, leftVolume, rightVolume, priority, loop, rate
            soundPool.play(successSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            Log.d(TAG, "Playing success sound. ID: " + successSoundId);
        } else {
            Log.w(TAG, "SoundPool not loaded or null, cannot play sound.");
            // Fallback: Hiển thị Toast nếu không phát được âm thanh
            Toast.makeText(WatchActivity.this, "Đăng thành công!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                // Kiểm tra xem editTextCommentInput có đang focus không
                if (v == editTextCommentInput) {
                    Rect outRect = new Rect();
                    v.getGlobalVisibleRect(outRect);
                    // Nếu chạm ra ngoài vùng của EditText đang focus
                    if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                        Log.d(TAG, "Touch outside EditText (dispatchTouchEvent), clearing focus and hiding keyboard.");
                        v.clearFocus(); // Xóa focus khỏi EditText
                        hideKeyboard(v);  // Gọi hàm ẩn bàn phím (truyền view EditText vào)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void setupToolbar() {
        if (toolbarWatchActivity != null) {
            setSupportActionBar(toolbarWatchActivity);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            toolbarWatchActivity.setNavigationOnClickListener(v -> onBackPressed());
        } else {
            Log.e(TAG, "Toolbar (toolbar_watch_activity) not found in layout!");
        }
    }

    private void setupRecyclerViews() {
        if (recyclerViewCastCrew != null) {
            recyclerViewCastCrew.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerViewCastCrew.setHasFixedSize(true);
        }

        if (recyclerViewRecommended != null) {
            recyclerViewRecommended.setLayoutManager(new GridLayoutManager(this, 3));
            // recyclerViewRecommended.setHasFixedSize(true); // Can cause issues with dynamic content height
            try {
                recyclerViewRecommended.addItemDecoration(new GridSpacingItemDecoration(3, 16));
            } catch (Exception e) {
                Log.w(TAG, "GridSpacingItemDecoration error.", e);
            }
            recommendedAdapter = new RecommendedMovieAdapter(new ArrayList<>(), this);
            recyclerViewRecommended.setAdapter(recommendedAdapter);
        }

        if (recyclerViewEpisodes != null) {
            recyclerViewEpisodes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerViewEpisodes.setHasFixedSize(true);
            episodeAdapter = new EpisodeAdapter(this, this);
            recyclerViewEpisodes.setAdapter(episodeAdapter);
        }

        if (recyclerViewComments != null) {
            recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewComments.setNestedScrollingEnabled(false);
            commentAdapter = new CommentAdapter(this, this);
            recyclerViewComments.setAdapter(commentAdapter);
            recyclerViewComments.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy > 0) {
                        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        if (layoutManager != null && commentAdapter != null && commentAdapter.getItemCount() > 0) {
                            int lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();
                            int totalItemCount = commentAdapter.getItemCount();
                            int threshold = 1;
                            if (lastVisibleItemPosition >= totalItemCount - 1 - threshold) {
                                if (!isLoadingComments && !isLastCommentPage && currentEpisodeIdForComments != null) {
                                    fetchCommentsForEpisode(currentEpisodeIdForComments, currentCommentPage + 1, false);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void setupFullscreenLauncher() {
        fullscreenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        long lastPosition = result.getData().getLongExtra(FullscreenPlayerActivity.RESULT_LAST_POSITION, 0);
                        float lastSpeed = result.getData().getFloatExtra(FullscreenPlayerActivity.RESULT_PLAYBACK_SPEED, 1.0f);
                        boolean qualityIsAuto = result.getData().getBooleanExtra(FullscreenPlayerActivity.RESULT_QUALITY_IS_AUTO, true);
                        int qualityRendererIndex = -1, qualityGroupIndex = -1, qualityTrackIndex = -1;

                        if (!qualityIsAuto) {
                            qualityRendererIndex = result.getData().getIntExtra(FullscreenPlayerActivity.RESULT_QUALITY_RENDERER_INDEX, -1);
                            qualityGroupIndex = result.getData().getIntExtra(FullscreenPlayerActivity.RESULT_QUALITY_GROUP_INDEX_IN_RENDERER_TRACK_GROUPS, -1);
                            qualityTrackIndex = result.getData().getIntExtra(FullscreenPlayerActivity.RESULT_QUALITY_TRACK_INDEX_IN_GROUP, -1);
                        }
                        startPositionToResume = lastPosition;
                        if (player == null) {
                            initializePlayer();
                        }
                        if (player != null) {
                            player.setPlaybackParameters(new PlaybackParameters(lastSpeed));
                            if (player.getTrackSelector() instanceof DefaultTrackSelector) {
                                DefaultTrackSelector trackSelector = (DefaultTrackSelector) player.getTrackSelector();
                                DefaultTrackSelector.Parameters.Builder paramsBuilder = trackSelector.getParameters().buildUpon();
                                MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                                if (mappedTrackInfo != null && qualityRendererIndex != -1) {
                                    TrackGroupArray rendererTrackGroups = mappedTrackInfo.getTrackGroups(qualityRendererIndex);
                                    if (qualityIsAuto) {
                                        paramsBuilder.clearSelectionOverrides(qualityRendererIndex);
                                    } else if (qualityGroupIndex != -1 && qualityTrackIndex != -1 && rendererTrackGroups != null) {
                                        DefaultTrackSelector.SelectionOverride override =
                                                new DefaultTrackSelector.SelectionOverride(qualityGroupIndex, qualityTrackIndex);
                                        paramsBuilder.setSelectionOverride(qualityRendererIndex, rendererTrackGroups, override);
                                    }
                                    trackSelector.setParameters(paramsBuilder.build());
                                } else if (qualityIsAuto && mappedTrackInfo != null) {
                                    for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                                        if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_VIDEO) {
                                            paramsBuilder.clearSelectionOverrides(i);
                                            break;
                                        }
                                    }
                                    trackSelector.setParameters(paramsBuilder.build());
                                }
                            }
                            if (player.getPlaybackState() == Player.STATE_IDLE) {
                                if (episodeUrl != null && !episodeUrl.isEmpty()) {
                                    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(episodeUrl));
                                    player.setMediaItem(mediaItem);
                                    player.prepare();
                                }
                            }
                            player.seekTo(startPositionToResume);
                            player.play();
                        }
                        if (playerView != null) {
                            playerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (player != null) {
                            player.play();
                        }
                    }
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                });
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
                    startPositionToResume = 0;
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
                    return;
                }
            }
            player.seekTo(startPositionToResume);
            startPositionToResume = 0;
        }
        if (player != null) {
            updatePlayPauseButtons(player.isPlaying());
        }
    }

    private void initializeDownloadFeature() {
        downloadViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);
        downloadViewModel.getDownloadedMoviesLiveData().observe(this, downloadedMovies -> {
            Log.d(TAG, "Downloaded movies list changed in LiveData, updating button state.");
            updateDownloadButtonState();
        });
    }

    private void setupDownloadStatusReceiver() {
        downloadStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received ACTION_DOWNLOAD_STATUS_CHANGED broadcast");
                if (downloadViewModel != null) {
                    Log.d(TAG, "Telling DownloadViewModel to reload movies.");
                    downloadViewModel.loadDownloadedMovies();
                }
            }
        };
    }

    private void setupOtherListeners() {
        btnPlay.setOnClickListener(v -> playVideo());
        btnToggleDescription.setOnClickListener(new View.OnClickListener() {
            boolean expanded = false;
            @Override
            public void onClick(View v) {
                if (expanded) {
                    textDescription1.setMaxLines(1);
                    textDescription1.setEllipsize(TextUtils.TruncateAt.END);
                    btnToggleDescription.setText(getString(R.string.view_more));
                } else {
                    textDescription1.setMaxLines(Integer.MAX_VALUE);
                    textDescription1.setEllipsize(null);
                    btnToggleDescription.setText(getString(R.string.hide));
                }
                expanded = !expanded;
            }
        });
        if (iconDownloadMovieButton != null) {
            iconDownloadMovieButton.setOnClickListener(v -> handleDownloadClick());
        }
        if (editTextCommentInput != null) {
            editTextCommentInput.setOnClickListener(v -> checkLoginAndFocusComment());
            editTextCommentInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    checkLoginAndFocusComment();
                }
            });
        }
        if (buttonSendComment != null) {
            buttonSendComment.setOnClickListener(v -> postNewCommentWithLoginCheck());
        }
    }

    @Override
    public void onLikeClicked(MovieCommentResponse comment, int position) {
        Toast.makeText(this, "Đã thích bình luận: " + comment.getContent().substring(0, Math.min(comment.getContent().length(), 10)) + "...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReplyClicked(MovieCommentResponse comment, int position, View itemView) { // itemView có thể không cần dùng tới
        if (!isUserLoggedIn()) {
            showLoginPromptDialog();
            return;
        }

        this.replyingToComment = comment; // Lưu lại comment đang được trả lời

        // Hiển thị ô nhập liệu chính (commentInputBox)
        showCommentInputBox(true); // Phương thức này bạn đã có

        if (editTextCommentInput != null) { // editTextCommentInput là R.id.commentInput
            editTextCommentInput.setHint("Trả lời " + comment.getUserName() + "...");
            editTextCommentInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editTextCommentInput, InputMethodManager.SHOW_IMPLICIT);
            }
            // Cuộn xuống cuối nếu cần, hoặc cuộn tới ô nhập liệu
            // scrollContent.smoothScrollTo(0, commentInputBox.getTop()); // Hoặc tương tự
        }
    }

    @Override
    public void onViewRepliesClicked(MovieCommentResponse parentComment, int positionInAdapter) {
        if (parentComment == null || parentComment.getId() == null) {
            Toast.makeText(this, "ID bình luận không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer parentId = parentComment.getId();

        // Kiểm tra xem commentAdapter có đang quản lý trạng thái này không
        // Ví dụ, thông qua các phương thức của adapter:
        if (commentAdapter.isRepliesExpanded(parentId)) {
            commentAdapter.collapseReplies(parentId, positionInAdapter);
        } else {
            // Kiểm tra xem đã fetch replies trước đó chưa
            List<MovieCommentResponse> cachedReplies = commentAdapter.getCachedReplies(parentId); // Lấy từ fetchedRepliesMap của adapter
            if (cachedReplies != null) {
                commentAdapter.expandReplies(parentId, cachedReplies, positionInAdapter);
            } else {
                // Fetch từ API
                if (parentComment.getTotalReply() == 0) {
                    Toast.makeText(this, "Không có trả lời nào.", Toast.LENGTH_SHORT).show();
                    commentAdapter.markAsExpandedWithNoReplies(parentId); // Đánh dấu để không fetch lại
                    return;
                }
                fetchAndDisplayReplies(parentComment, positionInAdapter);
            }
        }
    }
    private void fetchAndDisplayReplies(MovieCommentResponse parentComment, int positionInAdapter) {
        Integer parentId = parentComment.getId();
        // Hiển thị loading cho item đó nếu cần
        // commentAdapter.notifyItemChanged(positionInAdapter); // Để hiển thị trạng thái loading

        AuthApiService apiService = AuthRepository.getInstance().getApi();
        apiService.getMovieCommentReplies(parentId) // API lấy replies dựa trên parentId
                .enqueue(new Callback<SimpleResponse<List<MovieCommentResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<SimpleResponse<List<MovieCommentResponse>>> call,
                                           @NonNull Response<SimpleResponse<List<MovieCommentResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            List<MovieCommentResponse> replies = response.body().getData();
                            commentAdapter.cacheAndExpandReplies(parentId, replies, positionInAdapter);
                        } else {
                            Toast.makeText(WatchActivity.this, "Lỗi khi tải trả lời.", Toast.LENGTH_SHORT).show();
                            // commentAdapter.notifyItemChanged(positionInAdapter); // Reset trạng thái loading
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SimpleResponse<List<MovieCommentResponse>>> call, @NonNull Throwable t) {
                        Toast.makeText(WatchActivity.this, "Lỗi mạng khi tải trả lời.", Toast.LENGTH_SHORT).show();
                        // commentAdapter.notifyItemChanged(positionInAdapter); // Reset trạng thái loading
                    }
                });
    }

    private void adjustScrollableContentPadding() {
        if (pageContainerScrollableContent == null || commentInputBox == null) {
            Log.w(TAG, "adjustScrollableContentPadding: pageContainer or commentInputBox is null.");
            return;
        }
        if (commentInputBox.getVisibility() == View.VISIBLE) {
            if (commentInputBox.getHeight() == 0) {
                commentInputBox.post(() -> {
                    if (commentInputBox.getHeight() > 0) {
                        pageContainerScrollableContent.setPadding(
                                pageContainerScrollableContent.getPaddingLeft(),
                                pageContainerScrollableContent.getPaddingTop(),
                                pageContainerScrollableContent.getPaddingRight(),
                                commentInputBox.getHeight()
                        );
                    }
                });
            } else {
                pageContainerScrollableContent.setPadding(
                        pageContainerScrollableContent.getPaddingLeft(),
                        pageContainerScrollableContent.getPaddingTop(),
                        pageContainerScrollableContent.getPaddingRight(),
                        commentInputBox.getHeight()
                );
            }
        } else {
            pageContainerScrollableContent.setPadding(
                    pageContainerScrollableContent.getPaddingLeft(),
                    pageContainerScrollableContent.getPaddingTop(),
                    pageContainerScrollableContent.getPaddingRight(),
                    0
            );
        }
    }

    private void setupTabsAndScrollListener() {
        if (tabLayout == null || scrollContent == null || anchorEpisodes == null || anchorForYou == null || anchorComments == null || recyclerViewRecommended == null) {
            Log.e(TAG, "setupTabsAndScrollListener: One or more required views for ScrollSpy are null.");
            return;
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                final boolean isSelectionProgrammaticByScroll = mBlockScrollToAnchorForNextTabSelection;

                if (isSelectionProgrammaticByScroll) {
                    mBlockScrollToAnchorForNextTabSelection = false; // Tiêu thụ cờ, không cuộn đến anchor
                }

                // Nếu đây là một click thủ công mới (không phải do scroll listener chọn,
                // và không phải đang trong một hành động isTabClickScrolling trước đó)
                if (!isSelectionProgrammaticByScroll && !isTabClickScrolling) {
                    View targetView = null;
                    switch (tab.getPosition()) {
                        case 0: targetView = anchorEpisodes; break;
                        case 1: targetView = anchorForYou; break;
                        case 2: targetView = anchorComments; break;
                    }

                    if (targetView != null) {
                        isTabClickScrolling = true; // <<< ĐẶT CỜ NGAY LẬP TỨC KHI CLICK THỦ CÔNG
                        // để OnScrollChangedListener bỏ qua các sự kiện cuộn sắp tới.
                        final View finalTargetView = targetView;
                        finalTargetView.post(() -> { // post để lấy tọa độ Y chính xác sau khi layout (nếu cần)
                            int targetY = finalTargetView.getTop();
                            // Điều chỉnh offset nếu cần, ví dụ trừ đi chiều cao của TabLayout/AppBarLayout
                            // Hoặc một padding nhỏ để anchor không bị che khuất hoàn toàn.
                            int paddingTopToAvoidTab = (int) (16 * getResources().getDisplayMetrics().density); // Ví dụ padding
                            if (tabLayout != null && tabLayout.getHeight() > 0 && scrollContent.getTop() < tabLayout.getBottom()) {
                                // Cân nhắc trừ đi chiều cao của tabLayout nếu nó cố định và che một phần scroll view
                                // paddingTopToAvoidTab += tabLayout.getHeight(); // Hoặc một phần của nó
                            }
                            scrollContent.smoothScrollTo(0, targetY - paddingTopToAvoidTab);
                        });

                        // Reset isTabClickScrolling sau khi hành động cuộn có thể đã hoàn tất
                        scrollSyncHandler.removeCallbacksAndMessages(null);
                        scrollSyncHandler.postDelayed(() -> {
                            isTabClickScrolling = false;
                            // Đảm bảo rằng sau khi cuộn xong, OnScrollChangedListener có thể chạy lại
                            // để cập nhật tab nếu vị trí cuộn cuối cùng không khớp hoàn toàn với anchor.
                            // Tuy nhiên, điều này thường không cần thiết nếu scroll chính xác.
                        }, 400); // Có thể tăng nhẹ thời gian chờ nếu cuộn phức tạp
                    } else {
                        // Nếu không có targetView để cuộn tới (ít khi xảy ra với tab hợp lệ)
                        // nhưng đây là click thủ công, đảm bảo isTabClickScrolling được reset nếu nó đã được set.
                        // Tuy nhiên, với logic trên, nếu targetView là null, isTabClickScrolling chưa được set true.
                    }
                }

                // Logic hiển thị/ẩn commentInputBox luôn chạy dựa trên tab được chọn (tab parameter)
                if (commentInputBox != null) {
                    if (tab.getPosition() == 2) { // Tab "Bình luận"
                        showCommentInputBox(true);
                    } else {
                        showCommentInputBox(false);
                    }
                    adjustScrollableContentPadding();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Thường không cần xử lý gì đặc biệt ở đây
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Log.d(TAG, "Tab reselected: " + tab.getText());
                // Khi tab được chọn lại, bạn có thể muốn thực hiện hành động cuộn đến anchor
                // tương tự như khi chọn lần đầu, nhưng chỉ khi không phải do scroll listener gây ra.
                // Đặt mBlockScrollToAnchorForNextTabSelection = false để đảm bảo nếu onTabSelected được gọi lại, nó có thể cuộn.
                mBlockScrollToAnchorForNextTabSelection = false; // Cho phép cuộn nếu onTabSelected được gọi
                isTabClickScrolling = false; // Reset cờ này trước khi gọi onTabSelected để nó coi như một click mới
                onTabSelected(tab);
            }
        });

        scrollContent.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (isTabClickScrolling || tabLayout == null || scrollContent == null ||
                    anchorEpisodes == null || anchorForYou == null || anchorComments == null ||
                    recyclerViewRecommended == null) {
                return;
            }

            int scrollY = scrollContent.getScrollY();
            int activeThreshold = (int) (16 * getResources().getDisplayMetrics().density);

            int episodesTop = anchorEpisodes.getTop();
            int forYouTop = anchorForYou.getTop();
            int commentsAnchorTop = anchorComments.getTop();

            // --- ĐIỂM KÍCH HOẠT SỚM HƠN CHO TAB BÌNH LUẬN ---
            int earlyCommentsTriggerY = commentsAnchorTop; // Mặc định

            // Chỉ tính toán điểm trigger sớm hơn nếu recyclerViewRecommended hiển thị và có chiều cao
            // và đảm bảo nó không bị gọi quá sớm trước khi fetchRecommendedMovies hoàn tất và RV có kích thước.
            if (recyclerViewRecommended.getVisibility() == View.VISIBLE && recyclerViewRecommended.getHeight() > 0) {
                int recommendedViewTop = recyclerViewRecommended.getTop();
                int recommendedViewHeight = recyclerViewRecommended.getHeight();

                // Kích hoạt khi người dùng cuộn qua 1/3 chiều cao của mục "Đề xuất"
                int scrollPastOffset = recommendedViewHeight * 2 / 4;
                int calculatedTrigger = recommendedViewTop + scrollPastOffset;

                earlyCommentsTriggerY = Math.min(calculatedTrigger, commentsAnchorTop);
                // Đảm bảo trigger này không sớm hơn trigger của tab "For You"
                earlyCommentsTriggerY = Math.max(earlyCommentsTriggerY, forYouTop + activeThreshold + 1);
            }
            // --- KẾT THÚC ĐIỂM KÍCH HOẠT SỚM HƠN ---

            int newSelectedTab = -1;

            if (scrollY + activeThreshold >= earlyCommentsTriggerY) {
                newSelectedTab = 2; // Comments tab
            } else if (scrollY + activeThreshold >= forYouTop) {
                newSelectedTab = 1; // For You tab
            } else {
                newSelectedTab = 0; // Episodes tab (mặc định)
            }

            int currentSelectedTab = tabLayout.getSelectedTabPosition();
            if (newSelectedTab != -1 && newSelectedTab != currentSelectedTab) {
                TabLayout.Tab tabToSelect = tabLayout.getTabAt(newSelectedTab);
                if (tabToSelect != null) {
                    mBlockScrollToAnchorForNextTabSelection = true; // Đặt cờ để onTabSelected không cuộn
                    tabToSelect.select();
                    // Không cần reset mBlockScrollToAnchorForNextTabSelection ở đây, onTabSelected sẽ làm
                }
            }
        });
    }



    private void showCommentInputBox(boolean show) {
        if (commentInputBox == null) return;

        if (show && isUserLoggedIn()) { // Chỉ hiển thị nếu tab comments được chọn VÀ đã đăng nhập
            if (!isCommentInputBoxVisible) {
                commentInputBox.setVisibility(View.VISIBLE);
                isCommentInputBoxVisible = true;
                adjustScrollViewPadding(true); // Điều chỉnh padding khi box hiển thị
                // Tùy chọn: Cuộn xuống cuối danh sách bình luận nếu muốn
                // if (recyclerViewComments != null && commentAdapter != null && commentAdapter.getItemCount() > 0) {
                //     recyclerViewComments.smoothScrollToPosition(commentAdapter.getItemCount() - 1);
                // }
            }
        } else { // Đây là trường hợp show == false (ẩn ô bình luận)
            if (isCommentInputBoxVisible) {
                commentInputBox.setVisibility(View.GONE);
                isCommentInputBoxVisible = false;

                // --- THAY ĐỔI QUAN TRỌNG Ở ĐÂY ---
                // Chủ động xóa focus khỏi EditText trước khi ẩn bàn phím
                if (editTextCommentInput != null && editTextCommentInput.hasFocus()) {
                    Log.d(TAG, "CommentInputBox is being hidden, clearing focus from editTextCommentInput.");
                    editTextCommentInput.clearFocus();
                }
                // --- KẾT THÚC THAY ĐỔI ---

                hideKeyboard(); // Gọi hàm ẩn bàn phím (hàm này nên dùng getCurrentFocus() hoặc nhận view cụ thể)
                adjustScrollViewPadding(false); // Khôi phục padding khi box ẩn
                resetReplyState();
            }
        }
    }

    private void adjustScrollViewPadding(boolean commentBoxVisible) {
        if (scrollContent == null || commentInputBox == null || pageContainerScrollableContent == null) {
            return;
        }
        commentInputBox.post(() -> {
            int commentBoxHeight = commentBoxVisible ? commentInputBox.getHeight() : 0;
            if (pageContainerScrollableContent.getPaddingBottom() != commentBoxHeight) {
                pageContainerScrollableContent.setPadding(
                        pageContainerScrollableContent.getPaddingLeft(),
                        pageContainerScrollableContent.getPaddingTop(),
                        pageContainerScrollableContent.getPaddingRight(),
                        commentBoxHeight
                );
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
        if (btnPauseCustom != null) btnPauseCustom.setOnClickListener(v -> { if (player != null) player.pause(); });
        if (btnRewind != null) btnRewind.setOnClickListener(v -> handleRewind());
        if (btnFfwd != null) btnFfwd.setOnClickListener(v -> handleFastForward());
        if (btnFullscreen != null) btnFullscreen.setOnClickListener(v -> openFullscreenActivity());
        if (btnSettings != null) btnSettings.setOnClickListener(v -> handleSettings());
    }

    @OptIn(markerClass = UnstableApi.class)
    private void openFullscreenActivity() {
        if (player != null && episodeUrl != null && !episodeUrl.isEmpty()) {
            long currentPosition = player.getCurrentPosition();
            float currentSpeed = player.getPlaybackParameters().speed;
            player.pause();
            Intent intent = new Intent(this, FullscreenPlayerActivity.class);
            intent.putExtra(FullscreenPlayerActivity.EXTRA_VIDEO_URL, episodeUrl);
            intent.putExtra(FullscreenPlayerActivity.EXTRA_START_POSITION, currentPosition);
            intent.putExtra(FullscreenPlayerActivity.EXTRA_PLAYBACK_SPEED, currentSpeed);
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
                            TrackGroupArray rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
                            if (currentParams.hasSelectionOverride(rendererIndex, rendererTrackGroups)) {
                                DefaultTrackSelector.SelectionOverride override = currentParams.getSelectionOverride(rendererIndex, rendererTrackGroups);
                                if (override != null && override.tracks.length > 0) {
                                    isAuto = false;
                                    groupIndex = override.groupIndex;
                                    trackIndex = override.tracks[0];
                                }
                            }
                            break;
                        }
                    }
                }
                intent.putExtra(FullscreenPlayerActivity.EXTRA_QUALITY_IS_AUTO, isAuto);
                if (!isAuto) {
                    intent.putExtra(FullscreenPlayerActivity.EXTRA_QUALITY_RENDERER_INDEX, rendererIndex);
                    intent.putExtra(FullscreenPlayerActivity.EXTRA_QUALITY_GROUP_INDEX_IN_RENDERER_TRACK_GROUPS, groupIndex);
                    intent.putExtra(FullscreenPlayerActivity.EXTRA_QUALITY_TRACK_INDEX_IN_GROUP, trackIndex);
                }
            }
            fullscreenLauncher.launch(intent);
        }
    }

    private void handleRewind() {
        if (player != null) player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
    }

    private void handleFastForward() {
        if (player != null) {
            long duration = player.getDuration();
            if (duration != C.TIME_UNSET) {
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
            if (which == 0) showSpeedSelectionDialog();
            else if (which == 1) showQualitySelectionDialog();
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showSpeedSelectionDialog() {
        if (player == null) return;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        builder.setTitle("Chọn tốc độ phát");
        builder.setSingleChoiceItems(speedOptions, currentSpeedIndex, (dialog, which) -> {
            if (player != null) player.setPlaybackParameters(new PlaybackParameters(speedValues[which]));
            dialog.dismiss();
            Toast.makeText(WatchActivity.this, "Tốc độ: " + speedOptions[which], Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void showQualitySelectionDialog() {
        if (player == null || !(player.getTrackSelector() instanceof DefaultTrackSelector)) {
            Toast.makeText(this, "Không thể thay đổi chất lượng.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Không có lựa chọn chất lượng video.", Toast.LENGTH_SHORT).show();
            return;
        }
        androidx.media3.common.TrackGroup targetVideoTrackGroup = null;
        int targetVideoTrackGroupIndexInRenderer = -1;
        for (int i = 0; i < rendererTrackGroups.length; i++) {
            androidx.media3.common.TrackGroup currentGroup = rendererTrackGroups.get(i);
            if (currentGroup.length > 0) {
                targetVideoTrackGroup = currentGroup;
                targetVideoTrackGroupIndexInRenderer = i;
                break;
            }
        }
        if (targetVideoTrackGroup == null) {
            Toast.makeText(this, "Không tìm thấy TrackGroup video phù hợp.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> qualityLabels = new ArrayList<>();
        List<Integer> trackIndicesWithinTargetGroup = new ArrayList<>();
        qualityLabels.add("Tự động");
        trackIndicesWithinTargetGroup.add(-1);
        for (int i = 0; i < targetVideoTrackGroup.length; i++) {
            Format format = targetVideoTrackGroup.getFormat(i);
            String label = format.height + "p";
            if (format.bitrate != Format.NO_VALUE) {
                label += " (~" + (format.bitrate / 1000) + "kbps)";
            }
            qualityLabels.add(label);
            trackIndicesWithinTargetGroup.add(i);
        }
        if (qualityLabels.size() <= 1) {
            Toast.makeText(this, "Chỉ có một chất lượng video khả dụng.", Toast.LENGTH_SHORT).show();
            return;
        }
        int currentSelectedDialogIndex = 0;
        DefaultTrackSelector.Parameters currentParams = trackSelector.getParameters();
        if (currentParams.hasSelectionOverride(videoRendererIndex, rendererTrackGroups)) {
            DefaultTrackSelector.SelectionOverride exoplayerOverride = currentParams.getSelectionOverride(videoRendererIndex, rendererTrackGroups);
            if (exoplayerOverride != null && exoplayerOverride.groupIndex == targetVideoTrackGroupIndexInRenderer && exoplayerOverride.tracks.length > 0) {
                int selectedTrackIndexInGroup = exoplayerOverride.tracks[0];
                for (int i = 1; i < trackIndicesWithinTargetGroup.size(); i++) {
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
        final TrackGroupArray finalRendererTrackGroups = rendererTrackGroups;
        final int finalTargetVideoTrackGroupIndexInRenderer = targetVideoTrackGroupIndexInRenderer;
        builder.setSingleChoiceItems(qualityLabels.toArray(new CharSequence[0]), currentSelectedDialogIndex,
                (dialog, which) -> {
                    DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.getParameters().buildUpon();
                    int selectedTrackIndexInGroupFromDialog = trackIndicesWithinTargetGroup.get(which);
                    if (selectedTrackIndexInGroupFromDialog == -1) {
                        parametersBuilder.clearSelectionOverrides(finalVideoRendererIndex);
                    } else {
                        DefaultTrackSelector.SelectionOverride newExoPlayerOverride =
                                new DefaultTrackSelector.SelectionOverride(finalTargetVideoTrackGroupIndexInRenderer, selectedTrackIndexInGroupFromDialog);
                        parametersBuilder.setSelectionOverride(finalVideoRendererIndex, finalRendererTrackGroups, newExoPlayerOverride);
                    }
                    trackSelector.setParameters(parametersBuilder.build());
                    dialog.dismiss();
                    Toast.makeText(WatchActivity.this, "Chất lượng: " + qualityLabels.get(which), Toast.LENGTH_SHORT).show();
                });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

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
            initializePlayer();
        }
        if (player != null) {
            if (btnPlay.getVisibility() == View.GONE) {
                player.play();
            }
            updatePlayPauseButtons(player.isPlaying());
        }
        if (downloadViewModel != null) {
            downloadViewModel.loadDownloadedMovies();
        }
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
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            if (playerListener != null) player.removeListener(playerListener);
            startPositionToResume = player.getCurrentPosition();
            player.release();
            player = null;
            if (playerView != null) playerView.setPlayer(null);
        }
    }

    private void fetchMovieDetail(String slug) {
        AuthRepository.getInstance().getApi().getMovieDetail(slug).enqueue(new Callback<MovieDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieDetailResponse> call, @NonNull Response<MovieDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    MovieDetailResponse.Movie movie = response.body().data;
                    movieId = movie.id;
                    currentMovieTitle = movie.title;
                    currentCoverImageUrl = movie.coverImage;
                    membershipType=movie.membershipType;
                    textTitle.setText(movie.title);
                    String rating = movie.rating != null ? "★ " + movie.rating : "N/A";
                    String year = movie.releaseDate != null && movie.releaseDate.length() >= 4 ? movie.releaseDate.substring(0, 4) : "N/A";
                    textDescription.setText(String.format("%s | %s", rating, year));
                    textDescription1.setText(movie.description);
                    if(coverImage != null && coverImage.getVisibility() == View.VISIBLE) {
                        Glide.with(WatchActivity.this).load(movie.coverImage).placeholder(R.drawable.default_cover_image).error(R.drawable.default_cover_image).into(coverImage);
                    }
                    if (movie.episode != null && !movie.episode.isEmpty()) {
                        allEpisodesList.clear();
                        allEpisodesList.addAll(movie.episode);
                        setupEpisodeRangeButtons();
                        displayEpisodesForRange(currentRangeStart);
                        if (currentPlayingEpisodeSlug != null) {
                            episodeAdapter.setCurrentPlayingEpisode(currentPlayingEpisodeSlug);
                        }
                    }
                    List<CastCrew> castList = new ArrayList<>();
                    if (movie.director != null) castList.add(new CastCrew(movie.director.getName(), "Director", movie.director.getThumbnail()));
                    if (movie.actor != null) {
                        for (MovieDetailResponse.Actor actor : movie.actor) {
                            castList.add(new CastCrew(actor.getName(), "Actor", actor.getAvatar()));
                        }
                    }
                    if (recyclerViewCastCrew != null) {
                        recyclerViewCastCrew.setAdapter(new CastCrewAdapter(castList));
                    }
                    if (movieId != null) fetchRecommendedMovies(movieId);
                    if (downloadViewModel != null && movieId != null && !TextUtils.isEmpty(currentMovieSlug)) {
                        downloadViewModel.loadDownloadedMovies();
                    }
                } else {
                    Toast.makeText(WatchActivity.this, "Không lấy được chi tiết phim", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<MovieDetailResponse> call, @NonNull Throwable t) {
                Toast.makeText(WatchActivity.this, "Lỗi mạng khi tải chi tiết phim", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchEpisode(String slug) {
        AuthRepository.getInstance().getApi().getEpisode(slug).enqueue(new Callback<EpisodeResponse>() {
            @Override
            public void onResponse(@NonNull Call<EpisodeResponse> call, @NonNull Response<EpisodeResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    EpisodeResponse.Episode episode = response.body().data;
                    episodeUrl = constructEpisodePlaybackUrl(episode.getLink());
                    processedLink = constructEpisodeProcessedUrl(episode.getProcessedLink());
                    episodeId = episode.getId();
                    episodeNumber = episode.getNumber();
                    if (episode.getSlug() != null) {
                        currentPlayingEpisodeSlug = episode.getSlug();
                        if (episodeAdapter != null && episodeAdapter.getItemCount() > 0) {
                            episodeAdapter.setCurrentPlayingEpisode(currentPlayingEpisodeSlug);
                        }
                    }
                    if (btnPlay != null) btnPlay.setEnabled(true);
                    if (episode.getId() != null) {
                        loadCommentsForEpisode(episode.getId(), true);
                    }
                } else {
                    Toast.makeText(WatchActivity.this, "Không tải được link video", Toast.LENGTH_SHORT).show();
                    if (btnPlay != null) btnPlay.setEnabled(false);
                }
            }
            @Override
            public void onFailure(@NonNull Call<EpisodeResponse> call, @NonNull Throwable t) {
                Toast.makeText(WatchActivity.this, "Lỗi mạng khi tải link video", Toast.LENGTH_SHORT).show();
                if (btnPlay != null) btnPlay.setEnabled(false);
            }
        });
    }

    private String constructEpisodePlaybackUrl(String rawLink) {
        if (rawLink == null || rawLink.isEmpty()) return null;
        if (rawLink.startsWith("http://") || rawLink.startsWith("https://")) return rawLink;
        return AppConstants.DOMAIN + ":8080/videos/" + rawLink + "/master.m3u8";
    }

    private void fetchRecommendedMovies(Integer movieIdToFetch) {
        if (movieIdToFetch == null) return;
        if (progressBarRecommended != null) progressBarRecommended.setVisibility(View.VISIBLE);
        if (recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE);
        CallRecommender.getInstance().getApi().getRecommendedMovie(movieIdToFetch).enqueue(new Callback<RecommendedMovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecommendedMovieResponse> call, @NonNull Response<RecommendedMovieResponse> response) {
                if (progressBarRecommended != null) progressBarRecommended.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().similarMovies != null) {
                    List<RecommendedMovieResponse.RecommendedMovie> recommendedMovies = response.body().similarMovies;
                    if (recommendedMovies != null && !recommendedMovies.isEmpty()) {
                        if (recommendedAdapter != null) recommendedAdapter.updateMovies(recommendedMovies);
                        if (recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.VISIBLE);
                    } else {
                        if (recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE);
                    }
                } else {
                    if (recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(@NonNull Call<RecommendedMovieResponse> call, @NonNull Throwable t) {
                if (progressBarRecommended != null) progressBarRecommended.setVisibility(View.GONE);
                if (recyclerViewRecommended != null) recyclerViewRecommended.setVisibility(View.GONE);
            }
        });
    }

    private void loadCommentsForEpisode(int episodeId, boolean resetExisting) {
        currentEpisodeIdForComments = episodeId;
        if (resetExisting) {
            currentCommentPage = 0;
            isLastCommentPage = false;
            if (commentAdapter != null) commentAdapter.setComments(new ArrayList<>());
        }
        fetchCommentsForEpisode(episodeId, currentCommentPage, resetExisting);
    }

    private void fetchCommentsForEpisode(int episodeId, int pageToFetch, boolean isRefreshing) {
        if (isLoadingComments && !isRefreshing) return;
        isLoadingComments = true;
        if (progressBarComments != null && (isRefreshing || pageToFetch == 0)) progressBarComments.setVisibility(View.VISIBLE);
        if (textNoComments != null && (isRefreshing || pageToFetch == 0)) textNoComments.setVisibility(View.GONE);

        AuthRepository.getInstance().getApi().getMovieComments(episodeId, pageToFetch, 10)
                .enqueue(new Callback<SimpleResponse<PageableResponse<MovieCommentResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<SimpleResponse<PageableResponse<MovieCommentResponse>>> call,
                                           @NonNull Response<SimpleResponse<PageableResponse<MovieCommentResponse>>> response) {
                        isLoadingComments = false;
                        if (progressBarComments != null) progressBarComments.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            SimpleResponse<PageableResponse<MovieCommentResponse>> simpleResponse = response.body();
                            if (simpleResponse.getData() != null) {
                                PageableResponse<MovieCommentResponse> pageableData = simpleResponse.getData();
                                List<MovieCommentResponse> fetchedComments = pageableData.getContent();
                                if (commentAdapter != null) {
                                    if (isRefreshing) commentAdapter.setComments(fetchedComments);
                                    else if (fetchedComments != null && !fetchedComments.isEmpty()) commentAdapter.addComments(fetchedComments);
                                }
                                currentCommentPage = pageableData.getNumber();
                                isLastCommentPage = pageableData.isLast();
                                if (commentAdapter != null && commentAdapter.getItemCount() == 0) {
                                    if (textNoComments != null) {
                                        textNoComments.setText("Chưa có bình luận nào cho tập này.");
                                        textNoComments.setVisibility(View.VISIBLE);
                                    }
                                } else {
                                    if (textNoComments != null) textNoComments.setVisibility(View.GONE);
                                }
                            } else {
                                if (commentAdapter != null && commentAdapter.getItemCount() == 0 && textNoComments != null) textNoComments.setVisibility(View.VISIBLE);
                            }
                        } else {
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
                        if (progressBarComments != null) progressBarComments.setVisibility(View.GONE);
                        if (commentAdapter != null && commentAdapter.getItemCount() == 0 && textNoComments != null) {
                            textNoComments.setText("Lỗi mạng khi tải bình luận.");
                            textNoComments.setVisibility(View.VISIBLE);
                        }
                        Toast.makeText(WatchActivity.this, "Lỗi mạng khi tải bình luận.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        return prefs.getBoolean("isLoggedIn", true); // true là để test, nên là false trong thực tế
    }

    private boolean isUserLoggedInCheck() {
        String username = UserManager.getUsername(this);
        if(username.isEmpty()){
            return false;
        }
        else{
            return true;
        }
    }

    private void showLoginPromptDialog() {
        new AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setTitle("Yêu cầu đăng nhập")
                .setMessage("Bạn cần đăng nhập để sử dụng tính năng bình luận.")
                .setPositiveButton("Đăng nhập", (dialog, which) -> {
                    // Intent intent = new Intent(WatchActivity.this, LoginActivity.class);
                    // startActivity(intent);
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void checkLoginAndFocusComment() {
        if (!isUserLoggedIn()) {
            showLoginPromptDialog();
            if (editTextCommentInput != null) {
                editTextCommentInput.clearFocus();
                hideKeyboard();
            }
            showCommentInputBox(false);
        } else {
            showCommentInputBox(true);
            if (editTextCommentInput != null) {
                editTextCommentInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(editTextCommentInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void setupCommentInputBoxVisibilityListener() {
        if (commentInputBox == null || scrollContent == null || editTextCommentInput == null || pageContainerScrollableContent == null) {
            return;
        }
        editTextCommentInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                checkLoginAndFocusComment();
                v.postDelayed(() -> {
                    if (scrollContent != null && commentInputBox.getVisibility() == View.VISIBLE && pageContainerScrollableContent != null) {
                        scrollContent.smoothScrollTo(0, pageContainerScrollableContent.getHeight());
                    }
                }, 300);
            } else {
                if (commentInputBox.getVisibility() == View.GONE) {
                    hideKeyboard();
                }
            }
        });
    }

    private void postNewCommentWithLoginCheck() {
        if (!isUserLoggedIn()) {
            showLoginPromptDialog();
            return;
        }
        postNewComment();
    }

    // Trong WatchActivity.java
    private void postNewComment() { // Hoặc tên phương thức xử lý gửi của bạn
        if (editTextCommentInput == null) return; // editTextCommentInput là R.id.commentInput
        String commentContent = editTextCommentInput.getText().toString().trim();

        if (TextUtils.isEmpty(commentContent)) {
//            Toast.makeText(this, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEpisodeIdForComments == null) {
            Toast.makeText(this, "Không xác định được tập phim", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xác định parentId nếu đang trả lời một bình luận
        Integer parentId = null;
        if (this.replyingToComment != null) {
            // Nếu comment đang được trả lời là một reply (có parentCommentId riêng),
            // thì parentId của comment mới này sẽ là parentCommentId của replyingToComment (tức là comment cha gốc).
            // Nếu comment đang được trả lời là một comment cha (không có parentCommentId),
            // thì parentId của comment mới này sẽ là ID của replyingToComment.
            if (this.replyingToComment.getParentCommentId() != null) {
                // Đang trả lời một reply, parentId sẽ là ID của comment cha GỐC của reply đó.
                parentId = this.replyingToComment.getParentCommentId();
            } else {
                // Đang trả lời một comment cha.
                parentId = this.replyingToComment.getId();
            }
            Log.d(TAG, "Replying to comment. Parent ID for new comment: " + parentId);
        } else {
            Log.d(TAG, "Posting a new top-level comment.");
        }

        // Tạo đối tượng request
        // Giả định MovieCommentRequest có constructor (Integer episodeId, String content, Integer parentCommentId)
        // Nếu API của bạn yêu cầu thêm thông tin người dùng (ví dụ: username) trong body,
        // bạn cần thêm nó vào MovieCommentRequest và constructor.
         String username = UserManager.getUsername(this);
         MovieCommentRequest request = new MovieCommentRequest(currentEpisodeIdForComments, commentContent, parentId, username);
//        MovieCommentRequest request = new MovieCommentRequest(currentEpisodeIdForComments, commentContent, parentId, );

        // Hiển thị loading hoặc vô hiệu hóa nút gửi
        if (buttonSendComment != null) {
            buttonSendComment.setEnabled(false);
        }

        // HIỂN THỊ PROGRESSBAR, VÔ HIỆU HÓA NÚT GỬI
        if (buttonSendComment != null) {
            buttonSendComment.setEnabled(false);
            // Nếu ProgressBar nằm chồng lên nút gửi, bạn có thể ẩn nút gửi
            // buttonSendComment.setVisibility(View.INVISIBLE);
        }
        if (sendingCommentProgressBar != null) {
            sendingCommentProgressBar.setVisibility(View.VISIBLE);
        }
//        Toast.makeText(this, "Đang gửi bình luận...", Toast.LENGTH_SHORT).show();

        AuthRepository.getInstance().getApi().addMovieComment(request)
                .enqueue(new Callback<SimpleResponse>() { // Sửa ở đây: SimpleResponse, không phải SimpleResponse<MovieCommentResponse>
                    @Override
                    public void onResponse(@NonNull Call<SimpleResponse> call, @NonNull Response<SimpleResponse> response) {
                        if (buttonSendComment != null) buttonSendComment.setEnabled(true); // buttonSendComment là R.id.sendButton

                        if (sendingCommentProgressBar != null) {
                            sendingCommentProgressBar.setVisibility(View.GONE);
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            // Kiểm tra thêm mã thành công cụ thể từ SimpleResponse nếu cần, ví dụ:
                            // if (response.body().isSuccess()) { // Giả sử SimpleResponse có trường isSuccess
                            playSuccessSound();
//                            Toast.makeText(WatchActivity.this, "Đăng thành công!", Toast.LENGTH_SHORT).show();
                            editTextCommentInput.setText(""); // editTextCommentInput là R.id.commentInput
                            hideKeyboard();

                            resetReplyState(); // Hàm này bạn đã có để reset hint và replyingToComment

                            // Tải lại bình luận
                            Integer parentIdBeingRepliedTo = (replyingToComment != null) ?
                                    (replyingToComment.getParentCommentId() != null ? replyingToComment.getParentCommentId() : replyingToComment.getId())
                                    : null;

                            if (parentIdBeingRepliedTo != null) {
                                int parentPosition = commentAdapter.findPositionById(parentIdBeingRepliedTo);
                                if (parentPosition != -1) {
                                    MovieCommentResponse actualParentComment = commentAdapter.getCommentAtPosition(parentPosition);
                                    if (actualParentComment != null) {
                                        // Fetch lại replies cho parentActualComment để bao gồm comment mới
                                        // Đảm bảo hàm fetchAndDisplayReplies cũng làm mới cache trong adapter
                                        fetchAndDisplayReplies(actualParentComment, parentPosition);
                                    } else {
                                        // Nếu không tìm thấy parent comment trong adapter (trường hợp hiếm), tải lại tất cả
                                        loadCommentsForEpisode(currentEpisodeIdForComments, true);
                                    }
                                } else {
                                    // Không tìm thấy vị trí của parent comment, có thể nó chưa được load hoặc list đã thay đổi
                                    loadCommentsForEpisode(currentEpisodeIdForComments, true);
                                }
                            } else {
                                // Bình luận gốc mới, tải lại toàn bộ
                                loadCommentsForEpisode(currentEpisodeIdForComments, true);
                            }
                            // } else {
                            //     // Xử lý trường hợp SimpleResponse báo lỗi từ server (ví dụ: response.body().getMessage())
                            //     Toast.makeText(WatchActivity.this, "Lỗi: " + (response.body().getMessage() != null ? response.body().getMessage() : "Không thể đăng bình luận."), Toast.LENGTH_LONG).show();
                            // }
                        } else {

                            // Lỗi HTTP hoặc body rỗng
                            Toast.makeText(WatchActivity.this, "Lỗi đăng bình luận: " + response.code() + " " + response.message(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SimpleResponse> call, @NonNull Throwable t) {
                        if (buttonSendComment != null) buttonSendComment.setEnabled(true);
                        if (sendingCommentProgressBar != null) {
                            sendingCommentProgressBar.setVisibility(View.GONE);
                        }
                        Toast.makeText(WatchActivity.this, "Lỗi mạng khi đăng bình luận.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "postNewComment onFailure: ", t);
                    }
                });
    }

    // Hàm mới để reset trạng thái trả lời
    private void resetReplyState() {
        this.replyingToComment = null;
        if (editTextCommentInput != null && originalCommentInputHint != null) {
            editTextCommentInput.setHint(originalCommentInputHint);
        }
        // Có thể cần ẩn commentInputBox nếu không muốn nó luôn hiện sau khi gửi
        // showCommentInputBox(false); // Tùy theo luồng bạn muốn
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void updateDownloadButtonState() {
        if (iconDownloadMovieButton == null || episodeId == null || TextUtils.isEmpty(currentPlayingEpisodeSlug)) {
            if (iconDownloadMovieButton != null) {
                iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_default_tint), PorterDuff.Mode.SRC_IN);
            }
            return;
        }
        List<DownloadedMovie> allDownloads = downloadViewModel.getDownloadedMoviesLiveData().getValue();
        DownloadedMovie currentMovieInList = null;
        if (allDownloads != null) {
            for (DownloadedMovie downloadedMovie : allDownloads) {
                if (downloadedMovie.getId().equals(episodeId) && currentPlayingEpisodeSlug.equals(downloadedMovie.getSlug())) {
                    currentMovieInList = downloadedMovie;
                    break;
                }
            }
        }
        if (currentMovieInList != null) {
            String status = currentMovieInList.getDownloadStatus();
            iconDownloadMovieButton.setImageResource(R.drawable.ic_download);
            if (DownloadedMovie.STATUS_COMPLETED.equals(status)) {
                iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_completed_green), PorterDuff.Mode.SRC_IN);
            } else if (DownloadedMovie.STATUS_DOWNLOADING.equals(status) || DownloadedMovie.STATUS_PENDING.equals(status)) {
                iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_downloading_blue), PorterDuff.Mode.SRC_IN);
            } else if (DownloadedMovie.STATUS_FAILED.equals(status)) {
                iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_failed_red), PorterDuff.Mode.SRC_IN);
            } else {
                iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_default_tint), PorterDuff.Mode.SRC_IN);
            }
        } else {
            iconDownloadMovieButton.setImageResource(R.drawable.ic_download);
            iconDownloadMovieButton.setColorFilter(ContextCompat.getColor(this, R.color.download_icon_default_tint), PorterDuff.Mode.SRC_IN);
        }
    }

    private void handleDownloadClick() {
        if (movieId == null || TextUtils.isEmpty(currentMovieTitle) ||
                TextUtils.isEmpty(currentCoverImageUrl) || TextUtils.isEmpty(processedLink) ||
                TextUtils.isEmpty(currentMovieSlug) || episodeId == null || episodeNumber == null) { // Thêm kiểm tra episodeId và episodeNumber
            Toast.makeText(this, "Thông tin phim/tập chưa sẵn sàng để tải.", Toast.LENGTH_LONG).show();
            return;
        }
        DownloadedMovie movieToDownload = new DownloadedMovie(
                episodeId, currentMovieTitle, currentCoverImageUrl, processedLink, currentPlayingEpisodeSlug, episodeNumber, membershipType);
        pendingMovieToDownload = movieToDownload;
        if (checkAndRequestStoragePermission()) {
            if (downloadViewModel.startDownload(pendingMovieToDownload)) {
                Toast.makeText(this, "Đang chuẩn bị tải: " + pendingMovieToDownload.getTitle(), Toast.LENGTH_SHORT).show();
            }
            pendingMovieToDownload = null;
        }
    }

    private boolean checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // Android 6 to 9
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_DOWNLOAD_PERMISSION);
                return false;
            }
        }
        // For Android 10+ (API 29+), Scoped Storage is used. No direct WRITE_EXTERNAL_STORAGE needed for app-specific directory.
        // If saving to shared storage (MediaStore), different permissions like READ_MEDIA_VIDEO (API 33+) might be relevant,
        // but DownloadManager handles this well for common downloads.
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_DOWNLOAD_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingMovieToDownload != null) {
                    if(downloadViewModel.startDownload(pendingMovieToDownload)) {
                        Toast.makeText(this, "Đang chuẩn bị tải: " + pendingMovieToDownload.getTitle(), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Quyền lưu trữ bị từ chối. Không thể tải phim.", Toast.LENGTH_LONG).show();
            }
            pendingMovieToDownload = null;
        }
    }

    @Override
    public void onEpisodeClick(MovieDetailResponse.Episode episode) {
        Toast.makeText(this, "Chuyển sang: " + episode.getDescription(), Toast.LENGTH_SHORT).show();
        String newEpisodeUrl = constructEpisodePlaybackUrl(episode.getLink());
        String newProcessedLink = constructEpisodeProcessedUrl(episode.getProcessedLink());
        if (newEpisodeUrl != null) {
            episodeUrl = newEpisodeUrl;
            processedLink = newProcessedLink;
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
        if (rawProcessedLink == null || rawProcessedLink.isEmpty()) return null;
        if (rawProcessedLink.startsWith("http://") || rawProcessedLink.startsWith("https://")) return rawProcessedLink;
        return AppConstants.DOMAIN + ":8080/videos/" + rawProcessedLink;
    }

    private void setupEpisodeRangeButtons() {
        if (episodeRangeContainer == null) return;
        episodeRangeContainer.removeAllViews();
        if (allEpisodesList.isEmpty()) return;
        int totalEpisodes = allEpisodesList.size();
        for (int i = 0; i < totalEpisodes; i += EPISODES_PER_RANGE) {
            final int rangeStartForButton = i;
            int rangeEnd = Math.min(i + EPISODES_PER_RANGE - 1, totalEpisodes - 1);
            Button rangeButton = new Button(this);
            rangeButton.setText(String.format(Locale.getDefault(), "%d-%d", rangeStartForButton + 1, rangeEnd + 1));
            rangeButton.setTag(rangeStartForButton);
            rangeButton.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            int horizontalPaddingDp = 5, verticalPaddingDp = 2;
            float density = getResources().getDisplayMetrics().density;
            rangeButton.setPadding((int) (horizontalPaddingDp * density), (int) (verticalPaddingDp * density), (int) (horizontalPaddingDp * density), (int) (verticalPaddingDp * density));
            rangeButton.setAllCaps(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, (int) (6 * density), 0);
            rangeButton.setLayoutParams(params);
            rangeButton.setBackgroundResource(R.drawable.range_button_background_selector);
            rangeButton.setTextColor(ContextCompat.getColorStateList(this, R.color.range_button_text_color_selector));
            rangeButton.setOnClickListener(v -> {
                currentRangeStart = rangeStartForButton;
                displayEpisodesForRange(rangeStartForButton);
            });
            episodeRangeContainer.addView(rangeButton);
        }
        updateRangeButtonHighlight();
    }

    private void displayEpisodesForRange(int rangeStart) {
        if (allEpisodesList.isEmpty() || episodeAdapter == null) return;
        int end = Math.min(rangeStart + EPISODES_PER_RANGE, allEpisodesList.size());
        List<MovieDetailResponse.Episode> episodesToShow = new ArrayList<>();
        if (rangeStart < end) episodesToShow.addAll(allEpisodesList.subList(rangeStart, end));
        episodeAdapter.updateEpisodes(episodesToShow);
        if (currentPlayingEpisodeSlug != null) episodeAdapter.setCurrentPlayingEpisode(currentPlayingEpisodeSlug);
        updateRangeButtonHighlight();
    }

    private void updateRangeButtonHighlight() {
        if (episodeRangeContainer == null) return;
        for (int i = 0; i < episodeRangeContainer.getChildCount(); i++) {
            View child = episodeRangeContainer.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                Object tag = button.getTag();
                if (tag instanceof Integer) {
                    button.setSelected((Integer) tag == currentRangeStart);
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
            Intent intent = new Intent(context, WatchActivity.class);
            intent.putExtra("MOVIE_SLUG_ID", movie.slug);
            context.startActivity(intent);
        } else {
            if (context != null) Toast.makeText(context, "Cannot open movie content.", Toast.LENGTH_SHORT).show();
        }
    }
}