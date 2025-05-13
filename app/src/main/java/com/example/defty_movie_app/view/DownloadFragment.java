package com.example.defty_movie_app.view;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// Removed Button import
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // Import ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.DownloadedMovieAdapter;
import com.example.defty_movie_app.data.dto.DownloadedMovie;
import com.example.defty_movie_app.viewmodel.DownloadViewModel; // Import your ViewModel

// Removed JSON imports
import java.io.File;
import java.util.List; // Keep this

public class DownloadFragment extends Fragment implements DownloadedMovieAdapter.OnDownloadedMovieClickListener {

    private static final String TAG = "DownloadFragment";

    private RecyclerView recyclerViewDownloads;
    private DownloadedMovieAdapter adapter;
    private TextView emptyDownloadsTextView;
    // Removed tempDownloadButton

    private DownloadViewModel downloadViewModel; // ViewModel instance

    public DownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize ViewModel
        downloadViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);
        // Removed DownloadStorageManager initialization
        // Removed addHardcodedMovieToStorageIfNotExists() call
    }

    // Removed addHardcodedMovieToStorageIfNotExists method

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        recyclerViewDownloads = view.findViewById(R.id.recyclerViewDownloads);
        emptyDownloadsTextView = view.findViewById(R.id.emptyDownloadsTextView);
        // Removed tempDownloadButton initialization and its OnClickListener

        setupRecyclerView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observe LiveData from ViewModel
        downloadViewModel.getDownloadedMoviesLiveData().observe(getViewLifecycleOwner(), movies -> {
            Log.d(TAG, "LiveData updated with " + (movies != null ? movies.size() : 0) + " movies.");
            if (adapter != null) {
                adapter.setDownloadedMovies(movies);
                updateUIBasedOnMovieList(movies);
            }
        });
        // Initial load is handled by ViewModel constructor and onResume will refresh
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the list when the fragment becomes visible
        // This ensures it picks up changes made by DownloadCompletionReceiver
        downloadViewModel.loadDownloadedMovies();
    }

    private void setupRecyclerView() {
        if (getContext() == null) return;
        adapter = new DownloadedMovieAdapter(getContext(), this);
        recyclerViewDownloads.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewDownloads.setAdapter(adapter);
    }

    // Removed loadDownloadedMovies() method, ViewModel handles loading

    private void updateUIBasedOnMovieList(List<DownloadedMovie> movies) {
        if (getView() == null || emptyDownloadsTextView == null || recyclerViewDownloads == null) return;

        if (movies == null || movies.isEmpty()) {
            emptyDownloadsTextView.setVisibility(View.VISIBLE);
            recyclerViewDownloads.setVisibility(View.GONE);
            // Removed handling for clearAllDownloadsButtonContainer
        } else {
            emptyDownloadsTextView.setVisibility(View.GONE);
            recyclerViewDownloads.setVisibility(View.VISIBLE);
            // Removed handling for clearAllDownloadsButtonContainer
        }
    }

    // Removed startActualDownload method, ViewModel handles this
    // Removed checkStoragePermission method (ViewModel can have a helper, or calling fragment handles it)
    // Removed requestStoragePermission method
    // Removed onRequestPermissionsResult method

    @Override
    public void onMovieClicked(DownloadedMovie movie) {
        if (getContext() == null || movie == null) return;

        String status = movie.getDownloadStatus();
        if (status == null) {
            status = DownloadedMovie.STATUS_PENDING; // Default to pending if null for some reason
        }

        if (DownloadedMovie.STATUS_COMPLETED.equals(status) && movie.getLocalFilePath() != null) {
            File movieFile = new File(movie.getLocalFilePath());
            if (movieFile.exists()) {
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
                // Consider telling ViewModel to update status to FAILED or re-download
                movie.setDownloadStatus(DownloadedMovie.STATUS_FAILED);
                movie.setLocalFilePath(null);
                // Let ViewModel handle storage updates if necessary. For now, just refresh list.
                downloadViewModel.loadDownloadedMovies();
            }
        } else if (DownloadedMovie.STATUS_DOWNLOADING.equals(status)) {
            Toast.makeText(getContext(), "Phim '" + movie.getTitle() + "' đang được tải xuống.", Toast.LENGTH_SHORT).show();
        } else if (DownloadedMovie.STATUS_PENDING.equals(status) || DownloadedMovie.STATUS_FAILED.equals(status)) {
            // Removed specific check for hardcoded demo movie
            Toast.makeText(getContext(), "Phim '" + movie.getTitle() + "' đang chờ tải hoặc tải lỗi. Bạn có thể thử tải lại từ màn hình chi tiết phim.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Phim '" + movie.getTitle() + "' chưa sẵn sàng hoặc có lỗi.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClicked(DownloadedMovie movie, int position) {
        if (getContext() == null || downloadViewModel == null || movie == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa phim đã tải")
                .setMessage("Bạn có chắc muốn xóa '" + movie.getTitle() + "' khỏi danh sách và bộ nhớ máy (nếu đã tải)?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    downloadViewModel.deleteDownloadedMovie(movie); // Delegate to ViewModel
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}