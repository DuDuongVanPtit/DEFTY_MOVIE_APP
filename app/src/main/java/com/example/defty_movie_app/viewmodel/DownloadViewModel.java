package com.example.defty_movie_app.viewmodel; // Hoặc package của bạn

import android.Manifest;
import android.app.Application;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.defty_movie_app.data.dto.DownloadedMovie;
import com.example.defty_movie_app.utils.DownloadServiceHelper;
import com.example.defty_movie_app.utils.DownloadStorageManager;

import java.io.File;
import java.util.List;

public class DownloadViewModel extends AndroidViewModel {

    private static final String TAG = "DownloadViewModel";
    private final DownloadStorageManager downloadStorageManager;
    private final MutableLiveData<List<DownloadedMovie>> downloadedMoviesLiveData;
    private final Context appContext;

    public DownloadViewModel(@NonNull Application application) {
        super(application);
        appContext = application.getApplicationContext();
        downloadStorageManager = new DownloadStorageManager(appContext);
        downloadedMoviesLiveData = new MutableLiveData<>();
        loadDownloadedMovies();
    }

    public LiveData<List<DownloadedMovie>> getDownloadedMoviesLiveData() {
        return downloadedMoviesLiveData;
    }

    public void loadDownloadedMovies() {
        List<DownloadedMovie> movies = downloadStorageManager.getDownloadedMovies();
        downloadedMoviesLiveData.postValue(movies);
        Log.d(TAG, "Loaded " + (movies != null ? movies.size() : 0) + " movies into LiveData.");
    }

    // Sửa hàm này để trả về boolean
    public boolean startDownload(DownloadedMovie movieInfo) {
        if (movieInfo == null) {
            Log.e(TAG, "MovieInfo is null, cannot start download.");
            Toast.makeText(appContext, "Thông tin phim không hợp lệ để tải.", Toast.LENGTH_SHORT).show();
            return false; // Không bắt đầu tải
        }

        DownloadedMovie existingMovie = downloadStorageManager.findMovieByIdAndSlug(movieInfo.getId(), movieInfo.getSlug());

        if (existingMovie != null) {
            if (DownloadedMovie.STATUS_COMPLETED.equals(existingMovie.getDownloadStatus())) {
                Toast.makeText(appContext, "'" + existingMovie.getTitle() + "' đã được tải xong.", Toast.LENGTH_SHORT).show();
                // loadDownloadedMovies(); // Không cần thiết nếu chỉ thông báo
                return false; // Không bắt đầu tải mới
            } else if (DownloadedMovie.STATUS_DOWNLOADING.equals(existingMovie.getDownloadStatus())) {
                Toast.makeText(appContext, "'" + existingMovie.getTitle() + "' đang được tải.", Toast.LENGTH_SHORT).show();
                return false; // Không bắt đầu tải mới
            }
            movieInfo.setDownloadId(existingMovie.getDownloadId());
        }

        movieInfo.setDownloadStatus(DownloadedMovie.STATUS_PENDING);
        movieInfo.setDownloadId(-1);
        downloadStorageManager.addDownloadedMovie(movieInfo); // addOrUpdate
        loadDownloadedMovies();

        long downloadId = DownloadServiceHelper.startDownload(appContext, movieInfo.getVideoUrl(), movieInfo.getTitle(), movieInfo.getSlug());

        if (downloadId != -1) {
            movieInfo.setDownloadId(downloadId);
            movieInfo.setDownloadStatus(DownloadedMovie.STATUS_DOWNLOADING);
            downloadStorageManager.updateDownloadedMovie(movieInfo);
            Log.i(TAG, "Đã yêu cầu tải xuống cho: " + movieInfo.getTitle() + ", ID: " + downloadId);
            loadDownloadedMovies();
            return true; // Tải đã được bắt đầu (hoặc đang cố gắng bắt đầu)
        } else {
            movieInfo.setDownloadStatus(DownloadedMovie.STATUS_FAILED);
            movieInfo.setDownloadId(-1);
            downloadStorageManager.updateDownloadedMovie(movieInfo);
            Log.e(TAG, "Không thể bắt đầu tải xuống cho: " + movieInfo.getTitle());
            Toast.makeText(appContext, "Không thể bắt đầu tải: " + movieInfo.getTitle(), Toast.LENGTH_SHORT).show();
            loadDownloadedMovies();
            return false; // Không thể bắt đầu tải
        }
    }

    public void deleteDownloadedMovie(DownloadedMovie movie) {
        if (movie == null) return;

        String status = movie.getDownloadStatus();
        long currentDownloadId = movie.getDownloadId();

        if (currentDownloadId != -1 && status != null &&
                (DownloadedMovie.STATUS_DOWNLOADING.equals(status) || DownloadedMovie.STATUS_PENDING.equals(status))) {
            android.app.DownloadManager dm = (android.app.DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                int rowsAffected = dm.remove(currentDownloadId);
                Log.d(TAG, "Đã hủy tác vụ tải xuống ID: " + currentDownloadId + ", rows affected: " + rowsAffected);
            }
        }

        if (status != null && DownloadedMovie.STATUS_COMPLETED.equals(status) && movie.getLocalFilePath() != null) {
            File fileToDelete = new File(movie.getLocalFilePath());
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Log.d(TAG, "File vật lý đã xóa: " + movie.getLocalFilePath());
                } else {
                    Log.e(TAG, "Lỗi xóa file vật lý: " + movie.getLocalFilePath());
                    Toast.makeText(appContext, "Lỗi xóa file trên máy.", Toast.LENGTH_SHORT).show();
                }
            }
        }
        downloadStorageManager.removeDownloadedMovie(movie);
        loadDownloadedMovies();
        Toast.makeText(appContext, "Đã xóa: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
    }

    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            return ContextCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}