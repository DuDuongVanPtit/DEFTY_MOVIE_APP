package com.example.defty_movie_app.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.DownloadedMovieAdapter;
// Đảm bảo bạn đang import đúng package cho DownloadedMovie
import com.example.defty_movie_app.data.dto.DownloadedMovie;
import com.example.defty_movie_app.utils.DownloadServiceHelper;
import com.example.defty_movie_app.utils.DownloadStorageManager;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class DownloadFragment extends Fragment implements DownloadedMovieAdapter.OnDownloadedMovieClickListener {

    private static final String TAG = "DownloadFragment";
    private static final int REQUEST_CODE_WRITE_STORAGE_PERMISSION = 101;

    private RecyclerView recyclerViewDownloads;
    private DownloadedMovieAdapter adapter;
    private DownloadStorageManager downloadStorageManager;
    private TextView emptyDownloadsTextView;
    private Button clearAllDownloadsButton;
    private Button tempDownloadButton;

    private final String hardcodedMovieJson = "{\n" +
            "  \"status\": 200,\n" +
            "  \"message\": \"OK\",\n" +
            "  \"data\": {\n" +
            "    \"id\": 1,\n" +
            "    \"number\": 2,\n" +
            "    \"description\": \"Người Phán Xử - Tập Demo\",\n" +
            "    \"thumbnail\": \"https://res.cloudinary.com/drsmkfjfo/image/upload/v1743693793/1af608d0-8f67-4e46-83d9-811238352181_nguoiphanxu1.jpg\",\n" +
            "    \"link\": \"https://defty-video.s3.ap-southeast-2.amazonaws.com/a62aceb0-6e93-49b2-a6c6-71b9bc6d35a7Screen%20Recording%202024-08-24%20152825.mp4\",\n" +
            "    \"slug\": \"nguoi-phan-xu-37af2me2\",\n" +
            "    \"movieId\": null,\n" +
            "    \"status\": 1\n" +
            "  }\n" +
            "}";

    private DownloadedMovie pendingDownloadMovieInfo;


    public DownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadStorageManager = new DownloadStorageManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        recyclerViewDownloads = view.findViewById(R.id.recyclerViewDownloads);
        emptyDownloadsTextView = view.findViewById(R.id.emptyDownloadsTextView);
        clearAllDownloadsButton = view.findViewById(R.id.clearAllDownloadsButton);
        tempDownloadButton = view.findViewById(R.id.tempDownloadButtonInDownloadFragment);

        setupRecyclerView();

        if (clearAllDownloadsButton != null) {
            clearAllDownloadsButton.setOnClickListener(v -> confirmClearAllDownloads());
        }

        if (tempDownloadButton != null) {
            tempDownloadButton.setOnClickListener(v -> prepareAndStartDownload());
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadDownloadedMovies();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDownloadedMovies();
    }

    private void setupRecyclerView() {
        adapter = new DownloadedMovieAdapter(getContext(), this);
        recyclerViewDownloads.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewDownloads.setAdapter(adapter);
    }

    private void loadDownloadedMovies() {
        if (downloadStorageManager == null || adapter == null) return;
        List<DownloadedMovie> movies = downloadStorageManager.getDownloadedMovies();
        adapter.setDownloadedMovies(movies);
        if (movies.isEmpty()) {
            emptyDownloadsTextView.setVisibility(View.VISIBLE);
            recyclerViewDownloads.setVisibility(View.GONE);
            if (clearAllDownloadsButton != null) clearAllDownloadsButton.setVisibility(View.GONE);
        } else {
            emptyDownloadsTextView.setVisibility(View.GONE);
            recyclerViewDownloads.setVisibility(View.VISIBLE);
            if (clearAllDownloadsButton != null) clearAllDownloadsButton.setVisibility(View.VISIBLE);
        }
    }

    private void prepareAndStartDownload() {
        try {
            JSONObject root = new JSONObject(hardcodedMovieJson);
            JSONObject data = root.getJSONObject("data");

            int id = data.getInt("id");
            String title = data.getString("description");
            String thumbnail = data.getString("thumbnail");
            String videoUrl = data.getString("link");
            String slug = data.getString("slug");

            DownloadedMovie movieInfo = new DownloadedMovie(id, title, thumbnail, videoUrl, slug);
            this.pendingDownloadMovieInfo = movieInfo;

            if (checkStoragePermission()) {
                startActualDownload(movieInfo);
            } else {
                requestStoragePermission();
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi khi chuẩn bị tải xuống.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error preparing download", e);
        }
    }

    private void startActualDownload(DownloadedMovie movieInfo) {
        if (movieInfo == null || getContext() == null) {
            Log.e(TAG, "Không thể bắt đầu tải: movieInfo hoặc context là null.");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi: Thông tin phim không hợp lệ.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        List<DownloadedMovie> existingDownloads = downloadStorageManager.getDownloadedMovies();
        for (DownloadedMovie existing : existingDownloads) {
            if (existing.equals(movieInfo)) { // equals so sánh id và slug
                // Nếu phim đã tồn tại
                if (DownloadedMovie.STATUS_COMPLETED.equals(existing.getDownloadStatus())) {
                    Toast.makeText(getContext(), "'" + movieInfo.getTitle() + "' đã được tải xong.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (DownloadedMovie.STATUS_DOWNLOADING.equals(existing.getDownloadStatus()) && existing.getDownloadId() != -1) {
                    Toast.makeText(getContext(), "'" + movieInfo.getTitle() + "' đang được tải.", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    // Trạng thái PENDING, FAILED, hoặc PENDING nhưng chưa có downloadId
                    // -> Cho phép thử tải lại bằng cách xóa entry cũ và thêm entry mới với downloadId mới
                    Log.d(TAG, "Phim '" + movieInfo.getTitle() + "' tồn tại với trạng thái " + existing.getDownloadStatus() + ". Thử tải lại.");
                    downloadStorageManager.removeDownloadedMovie(existing); // Xóa entry cũ
                    break; // Thoát vòng lặp để tiếp tục quá trình tải mới
                }
            }
        }

        long downloadId = DownloadServiceHelper.startDownload(getContext(), movieInfo.getVideoUrl(), movieInfo.getTitle(), movieInfo.getSlug());

        if (downloadId != -1) {
            movieInfo.setDownloadId(downloadId);
            movieInfo.setDownloadStatus(DownloadedMovie.STATUS_DOWNLOADING);
            downloadStorageManager.addDownloadedMovie(movieInfo);
            loadDownloadedMovies();
            Log.i(TAG, "Đã thêm vào SharedPreferences và bắt đầu tải: " + movieInfo.getTitle());
        }
    }


    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Đã cấp quyền lưu trữ.", Toast.LENGTH_SHORT).show();
                if (pendingDownloadMovieInfo != null) {
                    startActualDownload(pendingDownloadMovieInfo);
                    pendingDownloadMovieInfo = null;
                }
            } else {
                Toast.makeText(getContext(), "Quyền lưu trữ bị từ chối. Không thể tải phim.", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public void onMovieClicked(DownloadedMovie movie) {
        String status = movie.getDownloadStatus();
        if (status == null) { // Xử lý trường hợp status là null cho phim cũ
            status = DownloadedMovie.STATUS_PENDING; // Hoặc một trạng thái mặc định
        }

        if (DownloadedMovie.STATUS_COMPLETED.equals(status) && movie.getLocalFilePath() != null) {
            Toast.makeText(getContext(), "Phát phim từ: " + movie.getLocalFilePath(), Toast.LENGTH_LONG).show();
            // TODO: Mở trình phát video với movie.getLocalFilePath()
        } else if (DownloadedMovie.STATUS_DOWNLOADING.equals(status)) {
            Toast.makeText(getContext(), "Phim '" + movie.getTitle() + "' đang được tải xuống.", Toast.LENGTH_SHORT).show();
        } else if (DownloadedMovie.STATUS_PENDING.equals(status)) {
            Toast.makeText(getContext(), "Phim '" + movie.getTitle() + "' đang chờ tải.", Toast.LENGTH_SHORT).show();
        } else { // Bao gồm cả STATUS_FAILED
            Toast.makeText(getContext(), "Phim '" + movie.getTitle() + "' chưa sẵn sàng hoặc tải lỗi.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClicked(DownloadedMovie movie, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa phim đã tải")
                .setMessage("Bạn có chắc muốn xóa '" + movie.getTitle() + "' khỏi danh sách và bộ nhớ máy (nếu đã tải)?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    String status = movie.getDownloadStatus();
                    long currentDownloadId = movie.getDownloadId();

                    // Xóa file vật lý nếu đã tải thành công
                    // Thêm kiểm tra status != null trước khi gọi equals
                    if (status != null && DownloadedMovie.STATUS_COMPLETED.equals(status) && movie.getLocalFilePath() != null) {
                        File fileToDelete = new File(movie.getLocalFilePath());
                        if (fileToDelete.exists()) {
                            if (fileToDelete.delete()) {
                                Log.d(TAG, "File vật lý đã xóa: " + movie.getLocalFilePath());
                            } else {
                                Log.e(TAG, "Lỗi xóa file vật lý: " + movie.getLocalFilePath());
                                Toast.makeText(getContext(), "Lỗi xóa file trên máy.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    // Nếu đang tải, có thể hủy tải bằng DownloadManager
                    // Thêm kiểm tra status != null và downloadId hợp lệ
                    if (currentDownloadId != -1 && status != null &&
                            (DownloadedMovie.STATUS_DOWNLOADING.equals(status) || DownloadedMovie.STATUS_PENDING.equals(status))) {
                        if (getContext() != null) {
                            android.app.DownloadManager dm = (android.app.DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                            if (dm != null) {
                                int rowsAffected = dm.remove(currentDownloadId);
                                Log.d(TAG, "Đã hủy tác vụ tải xuống ID: " + currentDownloadId + ", rows affected: " + rowsAffected);
                            }
                        }
                    }

                    // Xóa khỏi SharedPreferences (dựa trên equals của id và slug, nên sẽ hoạt động cho cả phim cũ và mới)
                    downloadStorageManager.removeDownloadedMovie(movie);
                    // Cập nhật adapter
                    if (adapter != null) { // Thêm kiểm tra null cho adapter
                        adapter.removeItem(position);
                    }

                    Toast.makeText(getContext(), "Đã xóa: " + movie.getTitle(), Toast.LENGTH_SHORT).show();

                    // Cập nhật UI nếu danh sách rỗng
                    if (adapter != null && adapter.getItemCount() == 0) {
                        emptyDownloadsTextView.setVisibility(View.VISIBLE);
                        recyclerViewDownloads.setVisibility(View.GONE);
                        if (clearAllDownloadsButton != null) clearAllDownloadsButton.setVisibility(View.GONE);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmClearAllDownloads() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa tất cả phim đã tải")
                .setMessage("Bạn có chắc muốn xóa toàn bộ danh sách và các file đã tải không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                    List<DownloadedMovie> movies = downloadStorageManager.getDownloadedMovies();
                    if (getContext() != null) {
                        android.app.DownloadManager dm = (android.app.DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                        for (DownloadedMovie movie : movies) {
                            String status = movie.getDownloadStatus();
                            // Xóa file vật lý
                            if (status != null && DownloadedMovie.STATUS_COMPLETED.equals(status) && movie.getLocalFilePath() != null) {
                                File fileToDelete = new File(movie.getLocalFilePath());
                                if (fileToDelete.exists()) {
                                    fileToDelete.delete();
                                }
                            }
                            // Hủy tác vụ tải nếu có
                            if (movie.getDownloadId() != -1 && dm != null) {
                                dm.remove(movie.getDownloadId());
                            }
                        }
                    }
                    downloadStorageManager.clearAllDownloads();
                    loadDownloadedMovies(); // Cập nhật UI
                    Toast.makeText(getContext(), "Đã xóa tất cả phim đã tải", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
