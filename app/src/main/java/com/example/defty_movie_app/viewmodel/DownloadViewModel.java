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

    public boolean startDownload(DownloadedMovie movieInfo) {
        if (movieInfo == null) {
            Log.e(TAG, "MovieInfo is null, cannot start download.");
            Toast.makeText(appContext, "Thông tin phim không hợp lệ để tải.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (movieInfo.getSlug() == null || movieInfo.getSlug().isEmpty()) {
            Log.e(TAG, "MovieInfo has invalid slug: " + movieInfo.getTitle());
            Toast.makeText(appContext, "Thông tin phim (slug) không hợp lệ.", Toast.LENGTH_SHORT).show();
            return false;
        }


        DownloadedMovie existingMovie = downloadStorageManager.findMovieByIdAndSlug(movieInfo.getId(), movieInfo.getSlug());

        if (existingMovie != null) {
            String status = existingMovie.getDownloadStatus();
            if (DownloadedMovie.STATUS_COMPLETED.equals(status)) {
                Toast.makeText(appContext, "'" + existingMovie.getTitle() + "' đã được tải xong.", Toast.LENGTH_SHORT).show();
                return false;
            } else if (DownloadedMovie.STATUS_DOWNLOADING.equals(status) || DownloadedMovie.STATUS_PENDING.equals(status)) {
                Toast.makeText(appContext, "'" + existingMovie.getTitle() + "' đang trong quá trình tải hoặc chờ tải.", Toast.LENGTH_SHORT).show();
                return false;
            }
            // Cho phép tải lại nếu là FAILED, PAUSED, CANCELLED
            movieInfo.setDownloadId(existingMovie.getDownloadId()); // Giữ lại downloadId cũ nếu có, mặc dù sẽ bị ghi đè
        }

        // Đặt lại trạng thái và ngày tháng cho một lần tải mới hoặc tải lại
        movieInfo.setDownloadStatus(DownloadedMovie.STATUS_PENDING);
        movieInfo.setDownloadDate(System.currentTimeMillis()); // Cập nhật ngày giờ thêm vào hàng đợi
        movieInfo.setLocalFilePath(null); // Xóa đường dẫn file cũ nếu có khi tải lại
        // addDownloadedMovie sẽ xử lý việc cập nhật hoặc thêm mới
        downloadStorageManager.addDownloadedMovie(movieInfo);
        // loadDownloadedMovies(); // Tải lại ngay để hiển thị trạng thái PENDING

        long downloadId = DownloadServiceHelper.startDownload(appContext, movieInfo.getVideoUrl(), movieInfo.getTitle(), movieInfo.getSlug());

        if (downloadId != -1) {
            movieInfo.setDownloadId(downloadId);
            movieInfo.setDownloadStatus(DownloadedMovie.STATUS_DOWNLOADING);
            downloadStorageManager.updateDownloadedMovie(movieInfo);
            Log.i(TAG, "Đã yêu cầu tải xuống cho: " + movieInfo.getTitle() + ", ID: " + downloadId);
            loadDownloadedMovies(); // Tải lại danh sách để cập nhật UI với trạng thái DOWNLOADING
            return true;
        } else {
            movieInfo.setDownloadStatus(DownloadedMovie.STATUS_FAILED);
            movieInfo.setDownloadId(-1);
            downloadStorageManager.updateDownloadedMovie(movieInfo);
            Log.e(TAG, "Không thể bắt đầu tải xuống cho: " + movieInfo.getTitle());
            Toast.makeText(appContext, "Không thể bắt đầu tải: " + movieInfo.getTitle(), Toast.LENGTH_SHORT).show();
            loadDownloadedMovies(); // Tải lại danh sách để cập nhật UI với trạng thái FAILED
            return false;
        }
    }

    public void cancelOngoingDownload(DownloadedMovie movie) {
        if (movie == null || movie.getDownloadId() == -1) {
            Log.w(TAG, "Không thể hủy tải: Thông tin phim không hợp lệ hoặc không có Download ID.");
            return;
        }

        String currentStatus = movie.getDownloadStatus();
        if (DownloadedMovie.STATUS_DOWNLOADING.equals(currentStatus) || DownloadedMovie.STATUS_PENDING.equals(currentStatus)) {
            DownloadManager dm = (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                int rowsAffected = dm.remove(movie.getDownloadId());
                Log.d(TAG, "Đã yêu cầu hủy tác vụ tải xuống ID: " + movie.getDownloadId() + ", rows affected: " + rowsAffected);
                if (rowsAffected > 0) {
                    Toast.makeText(appContext, "Đã hủy tải '" + movie.getTitle() + "'.", Toast.LENGTH_SHORT).show();
                    movie.setDownloadStatus(DownloadedMovie.STATUS_CANCELLED); // Sử dụng trạng thái CANCELLED
                } else {
                    // Có thể downloadId không còn hợp lệ (đã hoàn thành/thất bại trước đó và receiver đã xử lý)
                    Log.w(TAG, "Không hủy được tác vụ với Download ID: " + movie.getDownloadId() + ". Có thể đã hoàn thành/thất bại/bị xóa. Đặt là FAILED.");
                    // Nếu không hủy được từ DownloadManager (có thể do task đã kết thúc), vẫn cập nhật trạng thái
                    movie.setDownloadStatus(DownloadedMovie.STATUS_FAILED); // Hoặc CANCELLED tùy logic bạn muốn
                }
            } else {
                Log.e(TAG, "DownloadManager service không khả dụng khi cố gắng hủy tải.");
                movie.setDownloadStatus(DownloadedMovie.STATUS_FAILED); // Không có DM thì cũng coi như thất bại
            }
            downloadStorageManager.updateDownloadedMovie(movie);
            loadDownloadedMovies();
        } else {
            Log.d(TAG, "Phim '" + movie.getTitle() + "' không ở trạng thái đang tải (" + currentStatus + "), không thực hiện hủy.");
        }
    }

    public void deleteDownloadedMovie(DownloadedMovie movie) {
        if (movie == null) return;

        String status = movie.getDownloadStatus();
        long currentDownloadId = movie.getDownloadId();

        if (currentDownloadId != -1 &&
                (DownloadedMovie.STATUS_DOWNLOADING.equals(status) || DownloadedMovie.STATUS_PENDING.equals(status))) {
            DownloadManager dm = (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                int rowsAffected = dm.remove(currentDownloadId);
                Log.d(TAG, "Đã hủy tác vụ tải xuống ID: " + currentDownloadId + " khi xóa, rows affected: " + rowsAffected);
            }
        }

        if (movie.getLocalFilePath() != null) { // Kiểm tra file ngay cả khi không phải COMPLETED, vì có thể là FAILED/CANCELLED nhưng file tạm vẫn còn
            File fileToDelete = new File(movie.getLocalFilePath());
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Log.d(TAG, "File vật lý đã xóa: " + movie.getLocalFilePath());
                } else {
                    Log.w(TAG, "Không thể xóa file vật lý: " + movie.getLocalFilePath());
                }
            } else {
                Log.w(TAG, "File vật lý không tồn tại để xóa (khi thực hiện deleteDownloadedMovie): " + movie.getLocalFilePath());
            }
        }
        downloadStorageManager.removeDownloadedMovie(movie);
        loadDownloadedMovies();
        Toast.makeText(appContext, "Đã xóa: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
    }


    public void markDownloadAsFailed(DownloadedMovie movie) {
        if (movie == null) return;
        if (!DownloadedMovie.STATUS_FAILED.equals(movie.getDownloadStatus())) {
            Log.w(TAG, "Đánh dấu tải xuống thất bại cho: " + movie.getTitle() + ". File có thể đã bị xóa hoặc không truy cập được.");
            movie.setDownloadStatus(DownloadedMovie.STATUS_FAILED);
            movie.setLocalFilePath(null);
            downloadStorageManager.updateDownloadedMovie(movie);
            loadDownloadedMovies();
        }
    }


    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}