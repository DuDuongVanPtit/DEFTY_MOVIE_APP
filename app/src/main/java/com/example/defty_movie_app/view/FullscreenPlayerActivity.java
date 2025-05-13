package com.example.defty_movie_app.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

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

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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
import androidx.media3.ui.PlayerView;


import com.example.defty_movie_app.R; // Thay bằng package của bạn

import java.util.ArrayList;
import java.util.List;

public class FullscreenPlayerActivity extends AppCompatActivity {

    private static final String TAG = "FullscreenActivity";
    public static final String EXTRA_VIDEO_URL = "extra_video_url";
    public static final String EXTRA_START_POSITION = "extra_start_position";
    public static final String RESULT_LAST_POSITION = "result_last_position";

    // --- BEGIN: Thêm các key cho việc truyền cài đặt ---
    public static final String EXTRA_PLAYBACK_SPEED = "extra_playback_speed";
    public static final String EXTRA_QUALITY_IS_AUTO = "extra_quality_is_auto";
    public static final String EXTRA_QUALITY_RENDERER_INDEX = "extra_quality_renderer_index";
    public static final String EXTRA_QUALITY_GROUP_INDEX_IN_RENDERER_TRACK_GROUPS = "extra_quality_group_index_in_renderer_track_groups";
    public static final String EXTRA_QUALITY_TRACK_INDEX_IN_GROUP = "extra_quality_track_index_in_group";
    // --- END: Thêm các key cho việc truyền cài đặt ---

    // --- BEGIN: Thêm các key cho việc trả về kết quả cài đặt ---
    public static final String RESULT_PLAYBACK_SPEED = "result_playback_speed";
    public static final String RESULT_QUALITY_IS_AUTO = "result_quality_is_auto";
    public static final String RESULT_QUALITY_RENDERER_INDEX = "result_quality_renderer_index";
    public static final String RESULT_QUALITY_GROUP_INDEX_IN_RENDERER_TRACK_GROUPS = "result_quality_group_index_in_renderer_track_groups";
    public static final String RESULT_QUALITY_TRACK_INDEX_IN_GROUP = "result_quality_track_index_in_group";
    // --- END: Thêm các key cho việc trả về kết quả cài đặt ---


    // --- BEGIN: Biến lưu trữ cài đặt nhận được và cài đặt hiện tại ---
    private float initialSpeed = 1.0f;
    private boolean currentQualityIsAuto = true; // Mặc định là tự động
    private int currentQualityRendererIndex = -1;
    private int currentQualityGroupIndexInRenderer = -1;
    private int currentQualityTrackIndexInGroup = -1;
    // --- END: Biến lưu trữ cài đặt nhận được và cài đặt hiện tại ---


    private PlayerView playerView;
    private ExoPlayer player;
    private String videoUrl;
    private long startPosition = 0;

    // --- Thêm biến cho nút Play/Pause ---
    private ImageButton btnPlayCustom;
    private ImageButton btnPauseCustom;
    // --- ---
    private Player.Listener playerListener; // Listener để cập nhật UI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // --- Cài đặt Fullscreen UI ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        // --- ---

        setContentView(R.layout.activity_fullscreen_player);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // Buộc xoay ngang

        playerView = findViewById(R.id.fullscreen_player_view);

        // Lấy dữ liệu từ Intent
        videoUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        startPosition = getIntent().getLongExtra(EXTRA_START_POSITION, 0);

        // --- BEGIN: Nhận cài đặt từ Intent ---
        initialSpeed = getIntent().getFloatExtra(EXTRA_PLAYBACK_SPEED, 1.0f);
        currentQualityIsAuto = getIntent().getBooleanExtra(EXTRA_QUALITY_IS_AUTO, true);
        if (!currentQualityIsAuto) {
            currentQualityRendererIndex = getIntent().getIntExtra(EXTRA_QUALITY_RENDERER_INDEX, -1);
            currentQualityGroupIndexInRenderer = getIntent().getIntExtra(EXTRA_QUALITY_GROUP_INDEX_IN_RENDERER_TRACK_GROUPS, -1);
            currentQualityTrackIndexInGroup = getIntent().getIntExtra(EXTRA_QUALITY_TRACK_INDEX_IN_GROUP, -1);
        }
        // --- END: Nhận cài đặt từ Intent ---

        if (videoUrl == null || videoUrl.isEmpty()) {
            Log.e(TAG, "Video URL is missing!");
            Toast.makeText(this, "Lỗi: Không có URL video.", Toast.LENGTH_SHORT).show();
            finish(); // Đóng activity nếu không có URL
            return;
        }
        // Không gọi initializePlayer() ở đây nếu vòng đời chuẩn được dùng (onStart/onResume)
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer() {
        if (player == null && videoUrl != null && !videoUrl.isEmpty()) {
            try {
                // --- BEGIN: Tạo DefaultTrackSelector để có thể cài đặt chất lượng ---
                DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
                player = new ExoPlayer.Builder(this)
                        .setTrackSelector(trackSelector) // Sử dụng trackSelector vừa tạo
                        .build();
                // --- END: Tạo DefaultTrackSelector ---
                player = new ExoPlayer.Builder(this).build();
                playerView.setPlayer(player);

                // --- Khởi tạo và thêm Player Listener ---
                initializePlayerListener(); // Hàm mới để tạo listener
                player.addListener(playerListener); // Thêm listener vào player
                // --- ---

                MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
                player.setMediaItem(mediaItem);
                player.seekTo(startPosition); // Bắt đầu từ vị trí được truyền vào
                // --- BEGIN: Áp dụng tốc độ ban đầu ---
                player.setPlaybackParameters(new PlaybackParameters(initialSpeed));
                // --- END: Áp dụng tốc độ ban đầu ---
                player.prepare();
                Player.Listener qualityApplicationListener = new Player.Listener() {
                    @Override
                    public void onTracksChanged(@NonNull Tracks tracks) {
                        // MappedTrackInfo bây giờ nên có sẵn
                        applyInitialQualitySettings();
                        player.removeListener(this); // Xóa listener này sau khi áp dụng
                    }
                };
                player.addListener(qualityApplicationListener);
                // --- END: Áp dụng chất lượng ban đầu ---

                player.play();
                Log.d(TAG, "Player initialized. Speed: " + initialSpeed + ". Playing from: " + startPosition);
                setupCustomControlListeners_Fullscreen();

            } catch (Exception e) {
                Log.e(TAG, "Error initializing ExoPlayer", e);
                Toast.makeText(this, "Lỗi khởi tạo trình phát video.", Toast.LENGTH_SHORT).show();
                finishActivityWithResult(); // Gửi kết quả về dù lỗi
            }
        }
    }

    // --- BEGIN: Hàm mới để áp dụng cài đặt chất lượng ban đầu ---
    @OptIn(markerClass = UnstableApi.class)
    private void applyInitialQualitySettings() {
        if (player == null || !(player.getTrackSelector() instanceof DefaultTrackSelector)) {
            return;
        }
        DefaultTrackSelector trackSelector = (DefaultTrackSelector) player.getTrackSelector();
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

        if (mappedTrackInfo == null || currentQualityRendererIndex == -1) {
            // Nếu là Auto hoặc không có thông tin đầy đủ thì không làm gì, player sẽ tự chọn
            if (currentQualityIsAuto) {
                Log.d(TAG, "Applying initial quality: AUTO");
                DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.getParameters().buildUpon();
                // Cần tìm videoRendererIndex thực tế nếu muốn clear cho đúng renderer,
                // hoặc clear cho tất cả nếu không chắc chắn
                for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                    if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_VIDEO) {
                        parametersBuilder.clearSelectionOverrides(i);
                        break; // Giả sử chỉ có 1 video renderer chính
                    }
                }
                trackSelector.setParameters(parametersBuilder.build());
            }
            return;
        }

        // Áp dụng nếu không phải Auto và có đủ thông tin
        if (!currentQualityIsAuto && currentQualityGroupIndexInRenderer != -1 && currentQualityTrackIndexInGroup != -1) {
            TrackGroupArray rendererTrackGroups = mappedTrackInfo.getTrackGroups(currentQualityRendererIndex);
            if (rendererTrackGroups != null && currentQualityGroupIndexInRenderer < rendererTrackGroups.length) {
                androidx.media3.common.TrackGroup targetGroup = rendererTrackGroups.get(currentQualityGroupIndexInRenderer);
                if (targetGroup != null && currentQualityTrackIndexInGroup < targetGroup.length) {
                    DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.getParameters().buildUpon();
                    DefaultTrackSelector.SelectionOverride newExoPlayerOverride =
                            new DefaultTrackSelector.SelectionOverride(currentQualityGroupIndexInRenderer, currentQualityTrackIndexInGroup);
                    parametersBuilder.setSelectionOverride(currentQualityRendererIndex,
                            rendererTrackGroups,
                            newExoPlayerOverride);
                    trackSelector.setParameters(parametersBuilder.build());
                    Log.d(TAG, "Applied initial quality: Renderer " + currentQualityRendererIndex +
                            ", Group " + currentQualityGroupIndexInRenderer +
                            ", Track " + currentQualityTrackIndexInGroup);
                } else {
                    Log.w(TAG, "applyInitialQualitySettings: Track index out of bounds or targetGroup is null");
                }
            } else {
                Log.w(TAG, "applyInitialQualitySettings: Group index out of bounds or rendererTrackGroups is null");
            }
        }
    }

    // Hàm mới để khởi tạo Player.Listener
    private void initializePlayerListener() {
        playerListener = new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseButtons(isPlaying); // Gọi hàm cập nhật UI
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                // Có thể cần cập nhật thêm UI dựa trên state (buffering, ended, etc.)
                // Đồng thời cập nhật lại nút play/pause vì state thay đổi cũng ảnh hưởng
                if(player != null) { // Kiểm tra player không null
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


    // Sửa lại hàm này để tìm ID mới và đặt listener cho play/pause
    private void setupCustomControlListeners_Fullscreen() {
        if (playerView == null) return;

        // Tìm các nút bằng ID mới
        btnPlayCustom = playerView.findViewById(R.id.btn_play_custom);
        btnPauseCustom = playerView.findViewById(R.id.btn_pause_custom);
        ImageButton btnFullscreenExit = playerView.findViewById(R.id.btn_fullscreen_custom);
        ImageButton btnRewind = playerView.findViewById(R.id.btn_rewind_custom);
        ImageButton btnFfwd = playerView.findViewById(R.id.btn_ffwd_custom);
        ImageButton btnSettings = playerView.findViewById(R.id.btn_settings_custom); // Giữ lại nếu cần

        // Đặt listener cho Play/Pause thủ công
        if (btnPlayCustom != null) {
            btnPlayCustom.setOnClickListener(v -> {
                if (player != null) player.play();
            });
        } else {
            Log.w(TAG, "Custom Play button not found!");
        }

        if (btnPauseCustom != null) {
            btnPauseCustom.setOnClickListener(v -> {
                if (player != null) player.pause();
            });
        } else {
            Log.w(TAG, "Custom Pause button not found!");
        }

        // Giữ listener cho các nút khác
        if (btnFullscreenExit != null) {
            btnFullscreenExit.setOnClickListener(v -> finishActivityWithResult());
        } else {
            Log.w(TAG,"Fullscreen exit button not found!");
        }
        if (btnRewind != null) {
            btnRewind.setOnClickListener(v -> handleRewind());
        }
        if (btnFfwd != null) {
            btnFfwd.setOnClickListener(v -> handleFastForward());
        }
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> handleSettings()); // Giả sử bạn có hàm handleSettings()
        }
    }

    // Hàm tua tua trong fullscreen (giống WatchActivity)
    private void handleRewind() {
        if (player != null) {
            player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
        }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom); // Giả sử bạn có style này
        builder.setTitle("Cài đặt Video");
        builder.setItems(settingsOptions, (dialog, which) -> {
            if (which == 0) {
                showSpeedSelectionDialogFullscreen(); // Đổi tên hàm chọn tốc độ để tránh trùng
            } else if (which == 1) {
                showQualitySelectionDialog(); // Gọi hàm chọn chất lượng (sẽ tạo ở bước 5)
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    // Đổi tên hàm showSpeedSelectionDialog của bạn trong FullscreenPlayerActivity
    private void showSpeedSelectionDialogFullscreen() {
        // (Code chọn tốc độ của bạn ở đây, không thay đổi logic, chỉ đổi tên hàm)
        // ... giống hệt hàm handleSettings cũ của bạn, chỉ bỏ phần chọn chất lượng ra ...
        // Ví dụ:
        if (player == null) return;

        final CharSequence[] speedOptions = {"0.5x", "0.75x", "Bình thường (1x)", "1.25x", "1.5x", "2x"};
        final float[] speedValues = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        float currentSpeedVal = player.getPlaybackParameters().speed;
        int currentSpeedIndex = -1;
        for (int i = 0; i < speedValues.length; i++) {
            if (Math.abs(speedValues[i] - currentSpeedVal) < 0.01f) {
                currentSpeedIndex = i;
                break;
            }
        }
        if (currentSpeedIndex == -1) currentSpeedIndex = 2;

        AlertDialog.Builder speedBuilder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        speedBuilder.setTitle("Chọn tốc độ phát");
        speedBuilder.setSingleChoiceItems(speedOptions, currentSpeedIndex, (dialog, which) -> {
            if (player != null) {
                player.setPlaybackParameters(new PlaybackParameters(speedValues[which]));
            }
            dialog.dismiss();
            Toast.makeText(FullscreenPlayerActivity.this, "Tốc độ: " + speedOptions[which], Toast.LENGTH_SHORT).show();
        });
        speedBuilder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        speedBuilder.create().show();
    }

    // --- BEGIN: Hàm chọn chất lượng cho FullscreenPlayerActivity ---
    // (Sao chép gần như toàn bộ hàm showQualitySelectionDialog từ WatchActivity vào đây)
    // Đảm bảo rằng nó sử dụng this.player của FullscreenPlayerActivity
    // và cập nhật các biến currentQualityIsAuto, currentQualityRendererIndex, ... của Activity này
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

        androidx.media3.exoplayer.source.TrackGroupArray rendererTrackGroups = mappedTrackInfo.getTrackGroups(videoRendererIndex); // Sử dụng đúng kiểu TrackGroupArray
        if (rendererTrackGroups.isEmpty()) {
            Toast.makeText(this, "Không có lựa chọn chất lượng video (TrackGroupArray rỗng).", Toast.LENGTH_SHORT).show();
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

        if (qualityLabels.size() <= 1 && qualityLabels.get(0).equals("Tự động")) {
            Toast.makeText(this, "Chỉ có một chất lượng video khả dụng.", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentSelectedDialogIndex = 0;
        DefaultTrackSelector.Parameters currentParams = trackSelector.getParameters();

        // Cập nhật logic kiểm tra override hiện tại để khớp với các biến của FullscreenPlayerActivity
        if (!this.currentQualityIsAuto && this.currentQualityRendererIndex == videoRendererIndex &&
                this.currentQualityGroupIndexInRenderer == targetVideoTrackGroupIndexInRenderer) {
            for (int i = 1; i < trackIndicesWithinTargetGroup.size(); i++) {
                if (trackIndicesWithinTargetGroup.get(i) == this.currentQualityTrackIndexInGroup) {
                    currentSelectedDialogIndex = i;
                    break;
                }
            }
        } else if (this.currentQualityIsAuto) {
            currentSelectedDialogIndex = 0; // "Tự động"
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        builder.setTitle("Chọn chất lượng video");
        final int finalVideoRendererIndex = videoRendererIndex;
        final androidx.media3.exoplayer.source.TrackGroupArray finalRendererTrackGroups = rendererTrackGroups;
        final int finalTargetVideoTrackGroupIndexInRenderer = targetVideoTrackGroupIndexInRenderer;

        builder.setSingleChoiceItems(qualityLabels.toArray(new CharSequence[0]), currentSelectedDialogIndex,
                (dialog, which) -> {
                    DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.getParameters().buildUpon();
                    int selectedTrackIndexInGroupFromDialog = trackIndicesWithinTargetGroup.get(which);

                    if (selectedTrackIndexInGroupFromDialog == -1) { // "Tự động"
                        parametersBuilder.clearSelectionOverrides(finalVideoRendererIndex);
                        // Cập nhật trạng thái hiện tại của Activity
                        this.currentQualityIsAuto = true;
                        this.currentQualityRendererIndex = -1; // Hoặc giữ lại finalVideoRendererIndex nếu cần
                        this.currentQualityGroupIndexInRenderer = -1;
                        this.currentQualityTrackIndexInGroup = -1;
                    } else {
                        DefaultTrackSelector.SelectionOverride newExoPlayerOverride =
                                new DefaultTrackSelector.SelectionOverride(finalTargetVideoTrackGroupIndexInRenderer, selectedTrackIndexInGroupFromDialog);
                        parametersBuilder.setSelectionOverride(finalVideoRendererIndex,
                                finalRendererTrackGroups,
                                newExoPlayerOverride);
                        // Cập nhật trạng thái hiện tại của Activity
                        this.currentQualityIsAuto = false;
                        this.currentQualityRendererIndex = finalVideoRendererIndex;
                        this.currentQualityGroupIndexInRenderer = finalTargetVideoTrackGroupIndexInRenderer;
                        this.currentQualityTrackIndexInGroup = selectedTrackIndexInGroupFromDialog;
                    }
                    trackSelector.setParameters(parametersBuilder.build());
                    dialog.dismiss();
                    Toast.makeText(FullscreenPlayerActivity.this, "Chất lượng: " + qualityLabels.get(which), Toast.LENGTH_SHORT).show();
                });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
    // --- END: Hàm chọn chất lượng ---


    // Gửi kết quả (vị trí cuối cùng) về cho WatchActivity và đóng màn hình fullscreen
    private void finishActivityWithResult() {
        long lastPosition = player != null ? player.getCurrentPosition() : startPosition;
        Intent resultIntent = new Intent();
        resultIntent.putExtra(RESULT_LAST_POSITION, lastPosition);

        // --- BEGIN: Gửi trả cài đặt tốc độ và chất lượng ---
        if (player != null) {
            resultIntent.putExtra(RESULT_PLAYBACK_SPEED, player.getPlaybackParameters().speed);
        } else {
            resultIntent.putExtra(RESULT_PLAYBACK_SPEED, initialSpeed); // Gửi lại tốc độ ban đầu nếu player null
        }
        resultIntent.putExtra(RESULT_QUALITY_IS_AUTO, currentQualityIsAuto);
        if (!currentQualityIsAuto) {
            resultIntent.putExtra(RESULT_QUALITY_RENDERER_INDEX, currentQualityRendererIndex);
            resultIntent.putExtra(RESULT_QUALITY_GROUP_INDEX_IN_RENDERER_TRACK_GROUPS, currentQualityGroupIndexInRenderer);
            resultIntent.putExtra(RESULT_QUALITY_TRACK_INDEX_IN_GROUP, currentQualityTrackIndexInGroup);
        }
        // --- END: Gửi trả cài đặt ---

        setResult(RESULT_OK, resultIntent);
        Log.d(TAG, "Finishing fullscreen. Pos: " + lastPosition + ", Speed: " + (player != null ? player.getPlaybackParameters().speed : initialSpeed) + ", QualityAuto: " + currentQualityIsAuto);
        finish();
    }

    // --- Quản lý Vòng đời ExoPlayer cho Fullscreen ---
    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24) {
            initializePlayer();
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onResume() {
        super.onResume();
        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer();
        }
        if (player != null) {
            player.play(); // Đảm bảo video phát khi resume
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24 && player != null) {
            // Lưu vị trí trước khi pause nếu API < 24 vì player sẽ bị release ở onStop
            startPosition = player.getCurrentPosition();
            player.pause(); // Pause trước khi release ở onStop
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            if (player != null) startPosition = player.getCurrentPosition(); // Lưu vị trí trước khi release
            releasePlayer();
        }
    }

    // Luôn release player ở onDestroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            if (playerListener != null) {
                player.removeListener(playerListener); // <<< XÓA LISTENER
            }
            player.release();
            player = null;
            playerView.setPlayer(null);
            Log.d(TAG, "Fullscreen player released.");
        }
    }

    // Xử lý nút back vật lý
//    @Override
//    public void onBackPressed() {
//        finishActivityWithResult();
//        // super.onBackPressed(); // Không gọi super để tự xử lý việc đóng activity
//    }
}