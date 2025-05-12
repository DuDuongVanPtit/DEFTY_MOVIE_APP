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

import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.defty_movie_app.R; // Thay bằng package của bạn

public class FullscreenPlayerActivity extends AppCompatActivity {

    private static final String TAG = "FullscreenActivity";
    public static final String EXTRA_VIDEO_URL = "extra_video_url";
    public static final String EXTRA_START_POSITION = "extra_start_position";
    public static final String RESULT_LAST_POSITION = "result_last_position";

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
                player = new ExoPlayer.Builder(this).build();
                playerView.setPlayer(player);

                // --- Khởi tạo và thêm Player Listener ---
                initializePlayerListener(); // Hàm mới để tạo listener
                player.addListener(playerListener); // Thêm listener vào player
                // --- ---

                MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
                player.setMediaItem(mediaItem);
                player.seekTo(startPosition); // Bắt đầu từ vị trí được truyền vào
                player.prepare();
                player.play(); // Tự động phát khi vào fullscreen

                Log.d(TAG, "Player initialized and playing from: " + startPosition);

                setupCustomControlListeners_Fullscreen(); // Setup listener cho nút exit fullscreen

            } catch (Exception e) {
                Log.e(TAG, "Error initializing ExoPlayer", e);
                Toast.makeText(this, "Lỗi khởi tạo trình phát video.", Toast.LENGTH_SHORT).show();
                finishActivityWithResult(); // Gửi kết quả về dù lỗi
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

        // Các tùy chọn tốc độ phát
        final CharSequence[] speedOptions = {"0.5x", "0.75x", "Bình thường (1x)", "1.25x", "1.5x", "2x"};
        final float[] speedValues = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

        // Tìm tốc độ hiện tại để đánh dấu trong dialog
        float currentSpeed = player.getPlaybackParameters().speed;
        int currentSpeedIndex = -1;
        for (int i = 0; i < speedValues.length; i++) {
            if (Math.abs(speedValues[i] - currentSpeed) < 0.01f) { // So sánh float với sai số nhỏ
                currentSpeedIndex = i;
                break;
            }
        }
        if (currentSpeedIndex == -1) { // Nếu không tìm thấy tốc độ khớp chính xác, mặc định là Normal
            currentSpeedIndex = 2; // Index của "Bình thường (1x)"
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn tốc độ phát");
        builder.setSingleChoiceItems(speedOptions, currentSpeedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Người dùng đã chọn một tốc độ mới
                float selectedSpeed = speedValues[which];
                if (player != null) {
                    player.setPlaybackParameters(new PlaybackParameters(selectedSpeed));
                }
                dialog.dismiss(); // Đóng dialog sau khi chọn
                Toast.makeText(FullscreenPlayerActivity.this, "Tốc độ: " + speedOptions[which], Toast.LENGTH_SHORT).show();
            }
        });

        // Nút Hủy (không làm gì cả, chỉ đóng dialog)
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    // Gửi kết quả (vị trí cuối cùng) về cho WatchActivity và đóng màn hình fullscreen
    private void finishActivityWithResult() {
        long lastPosition = player != null ? player.getCurrentPosition() : startPosition; // Lấy vị trí cuối cùng
        Intent resultIntent = new Intent();
        resultIntent.putExtra(RESULT_LAST_POSITION, lastPosition);
        setResult(RESULT_OK, resultIntent);
        Log.d(TAG, "Finishing fullscreen, returning position: " + lastPosition);
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