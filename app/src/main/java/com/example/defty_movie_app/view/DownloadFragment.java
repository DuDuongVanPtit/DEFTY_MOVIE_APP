package com.example.defty_movie_app.view;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.DownloadedMovieAdapter;
import com.example.defty_movie_app.data.dto.DownloadedMovie;
import com.example.defty_movie_app.utils.DownloadCompletionReceiver;
import com.example.defty_movie_app.viewmodel.DownloadViewModel;

import java.io.File;
import java.util.List;

public class DownloadFragment extends Fragment implements DownloadedMovieAdapter.OnDownloadedMovieClickListener {

    private static final String TAG = "DownloadFragment";

    private RecyclerView recyclerViewDownloads;
    private DownloadedMovieAdapter adapter;
    private TextView emptyDownloadsTextView;

    private DownloadViewModel downloadViewModel;
    private BroadcastReceiver downloadStatusUpdateReceiver;

    public DownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);

        downloadStatusUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DownloadCompletionReceiver.ACTION_DOWNLOAD_STATUS_CHANGED.equals(intent.getAction())) {
                    Log.d(TAG, "Received ACTION_DOWNLOAD_STATUS_CHANGED in DownloadFragment. Reloading movies.");
                    if (downloadViewModel != null) {
                        downloadViewModel.loadDownloadedMovies();
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
    }

    @Override
    public void onStart() {
        super.onStart();
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
        // Luôn tải lại danh sách khi fragment resume để đảm bảo dữ liệu mới nhất
        if (downloadViewModel != null) {
            downloadViewModel.loadDownloadedMovies();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
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
        if (getContext() == null || movie == null || downloadViewModel == null) return;

        String status = movie.getDownloadStatus();
        if (status == null) {
            status = DownloadedMovie.STATUS_PENDING; // Trạng thái mặc định
        }

        switch (status) {
            case DownloadedMovie.STATUS_COMPLETED:
                playMovie(movie);
                break;
            case DownloadedMovie.STATUS_DOWNLOADING:
                new AlertDialog.Builder(requireContext())
                        .setTitle("Hủy tải xuống?")
                        .setMessage("Bạn có chắc muốn hủy tải xuống phim '" + movie.getTitle() + "' không?")
                        .setPositiveButton("Hủy tải", (dialog, which) -> {
                            downloadViewModel.cancelOngoingDownload(movie);
                            // Toast đã được hiển thị trong ViewModel hoặc có thể thêm ở đây nếu muốn
                        })
                        .setNegativeButton("Không", null)
                        .show();
                break;
            case DownloadedMovie.STATUS_PENDING:
                Toast.makeText(getContext(), "Phim '" + movie.getTitle() + "' đang chờ tải.", Toast.LENGTH_SHORT).show();
                break;
            case DownloadedMovie.STATUS_FAILED:
                Toast.makeText(getContext(), "Tải phim '" + movie.getTitle() + "' thất bại. Vui lòng xóa và thử lại.", Toast.LENGTH_LONG).show();
                break;
            case DownloadedMovie.STATUS_PAUSED:
                Toast.makeText(getContext(), "Tải phim '" + movie.getTitle() + "' đã tạm dừng.", Toast.LENGTH_SHORT).show();
                break;
            case DownloadedMovie.STATUS_CANCELLED:
                Toast.makeText(getContext(), "Tải phim '" + movie.getTitle() + "' đã được hủy.", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(getContext(), "Trạng thái phim '" + movie.getTitle() + "' (" + status + ") không xác định.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onDeleteClicked(DownloadedMovie movie, int position) {
        if (getContext() == null || downloadViewModel == null || movie == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa phim")
                .setMessage("Bạn có chắc muốn xóa '" + movie.getTitle() + "'? Hành động này sẽ xóa phim khỏi danh sách và xóa file đã tải (nếu có).")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    downloadViewModel.deleteDownloadedMovie(movie);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onWatchNowClicked(DownloadedMovie movie) {
        Log.d(TAG, "Watch Now clicked for: " + movie.getTitle());
        if (movie == null || getContext() == null) return;

        if (DownloadedMovie.STATUS_COMPLETED.equals(movie.getDownloadStatus())) {
            playMovie(movie);
        } else {
            Toast.makeText(getContext(), "Phim '" + movie.getTitle() + "' chưa tải xong.", Toast.LENGTH_SHORT).show();
        }
    }

    private void playMovie(DownloadedMovie movie) {
        if (getContext() == null || movie == null || movie.getLocalFilePath() == null) {
            Toast.makeText(getContext(), "Không thể phát phim, đường dẫn không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        File movieFile = new File(movie.getLocalFilePath());
        if (movieFile.exists() && movieFile.isFile()) { // Kiểm tra là file
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
            Toast.makeText(getContext(), "File phim không tồn tại hoặc không phải là file hợp lệ.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Movie file not found or not a file at: " + movie.getLocalFilePath());
            if (downloadViewModel != null) {
                downloadViewModel.markDownloadAsFailed(movie);
            }
        }
    }
}