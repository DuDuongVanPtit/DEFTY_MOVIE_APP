package com.example.defty_movie_app.view;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver; // Thêm import
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter; // Thêm import
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // Thêm import
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.DownloadedMovieAdapter;
import com.example.defty_movie_app.data.dto.DownloadedMovie;
import com.example.defty_movie_app.utils.DownloadCompletionReceiver; // Thêm import để lấy ACTION
import com.example.defty_movie_app.viewmodel.DownloadViewModel;

import java.io.File;
import java.util.List;

public class DownloadFragment extends Fragment implements DownloadedMovieAdapter.OnDownloadedMovieClickListener {

    private static final String TAG = "DownloadFragment";

    private RecyclerView recyclerViewDownloads;
    private DownloadedMovieAdapter adapter;
    private TextView emptyDownloadsTextView;

    private DownloadViewModel downloadViewModel;
    private BroadcastReceiver downloadStatusUpdateReceiver; // Thêm BroadcastReceiver

    public DownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);

        // Khởi tạo BroadcastReceiver
        downloadStatusUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DownloadCompletionReceiver.ACTION_DOWNLOAD_STATUS_CHANGED.equals(intent.getAction())) {
                    Log.d(TAG, "Received ACTION_DOWNLOAD_STATUS_CHANGED in DownloadFragment. Reloading movies.");
                    if (downloadViewModel != null) {
                        downloadViewModel.loadDownloadedMovies(); // Yêu cầu ViewModel tải lại
                    }
                }
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        recyclerViewDownloads = view.findViewById(R.id.recyclerViewDownloads);
        emptyDownloadsTextView = view.findViewById(R.id.emptyDownloadsTextView);
        setupRecyclerView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        downloadViewModel.getDownloadedMoviesLiveData().observe(getViewLifecycleOwner(), movies -> {
            Log.d(TAG, "LiveData updated with " + (movies != null ? movies.size() : 0) + " movies.");
            if (adapter != null) {
                adapter.setDownloadedMovies(movies);
                updateUIBasedOnMovieList(movies);
            }
        });
        // ViewModel đã tự load lần đầu trong constructor hoặc khi LiveData active
        // onResume sẽ load lại để đảm bảo cập nhật khi fragment quay lại
    }

    @Override
    public void onStart() {
        super.onStart();
        // Đăng ký BroadcastReceiver
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                    downloadStatusUpdateReceiver,
                    new IntentFilter(DownloadCompletionReceiver.ACTION_DOWNLOAD_STATUS_CHANGED)
            );
        }
        Log.d(TAG, "DownloadStatusUpdateReceiver registered.");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại danh sách khi fragment hiển thị lại, phòng trường hợp
        // broadcast bị lỡ khi fragment không active.
        // Nếu broadcast đã được nhận và xử lý rồi thì việc load lại này cũng không sao,
        // ViewModel có thể có cơ chế tránh load thừa nếu dữ liệu không đổi.
        if (downloadViewModel != null) {
            downloadViewModel.loadDownloadedMovies();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Hủy đăng ký BroadcastReceiver
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(downloadStatusUpdateReceiver);
        }
        Log.d(TAG, "DownloadStatusUpdateReceiver unregistered.");
    }

    private void setupRecyclerView() {
        if (getContext() == null) return;
        adapter = new DownloadedMovieAdapter(getContext(), this);
        recyclerViewDownloads.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewDownloads.setAdapter(adapter);
    }

    private void updateUIBasedOnMovieList(List<DownloadedMovie> movies) {
        if (getView() == null || emptyDownloadsTextView == null || recyclerViewDownloads == null) return;

        if (movies == null || movies.isEmpty()) {
            emptyDownloadsTextView.setVisibility(View.VISIBLE);
            recyclerViewDownloads.setVisibility(View.GONE);
        } else {
            emptyDownloadsTextView.setVisibility(View.GONE);
            recyclerViewDownloads.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMovieClicked(DownloadedMovie movie) {
        if (getContext() == null || movie == null) return;

        String status = movie.getDownloadStatus();
        if (status == null) {
            // Nếu trạng thái null, có thể coi là PENDING hoặc một trạng thái lỗi không xác định
            // Dựa theo logic cập nhật màu, PENDING sẽ hiển thị màu xanh dương
            status = DownloadedMovie.STATUS_PENDING;
        }

        if (DownloadedMovie.STATUS_COMPLETED.equals(status) && movie.getLocalFilePath() != null) {
            File movieFile = new File(movie.getLocalFilePath());
            if (movieFile.exists()) {
                // Kiểm tra xem context có còn hợp lệ không trước khi dùng requireContext()
                if (getActivity() == null || getActivity().isFinishing()) return;

                Uri videoUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", movieFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(videoUri, "video/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getContext(), "Không tìm thấy ứng dụng nào để phát video.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "No activity found to handle video intent for URI: " + videoUri, e);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Lỗi khi mở video.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error opening video with URI: " + videoUri, e);
                }
            } else {
                Toast.makeText(getContext(), "File phim không tồn tại trên thiết bị.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Movie file not found at: " + movie.getLocalFilePath());
                // Cân nhắc cập nhật trạng thái trong ViewModel
                movie.setDownloadStatus(DownloadedMovie.STATUS_FAILED);
                movie.setLocalFilePath(null); // Xóa đường dẫn file không hợp lệ
                if (downloadViewModel != null) {
                    // Thay vì chỉ load lại, nên có một hàm update cụ thể trong ViewModel
                    // downloadViewModel.updateMovieStatus(movie); // Ví dụ
                    downloadViewModel.loadDownloadedMovies(); // Tạm thời load lại toàn bộ danh sách
                }
            }
        } else if (DownloadedMovie.STATUS_DOWNLOADING.equals(status)) {
            Toast.makeText(getContext(), "Phim '" + movie.getTitle() + "' đang được tải xuống.", Toast.LENGTH_SHORT).show();
        } else if (DownloadedMovie.STATUS_PENDING.equals(status)) {
            Toast.makeText(getContext(), "Phim '" + movie.getTitle() + "' đang chờ tải.", Toast.LENGTH_SHORT).show();
        } else if (DownloadedMovie.STATUS_FAILED.equals(status)) {
            Toast.makeText(getContext(), "Tải phim '" + movie.getTitle() + "' thất bại. Vui lòng thử lại từ chi tiết phim.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Phim '" + movie.getTitle() + "' chưa sẵn sàng hoặc có lỗi ("+ status +").", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClicked(DownloadedMovie movie, int position) {
        if (getContext() == null || downloadViewModel == null || movie == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa phim đã tải")
                .setMessage("Bạn có chắc muốn xóa '" + movie.getTitle() + "' khỏi danh sách và bộ nhớ máy (nếu đã tải)?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    downloadViewModel.deleteDownloadedMovie(movie);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}