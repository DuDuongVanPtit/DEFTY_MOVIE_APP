package com.example.defty_movie_app.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

// Thêm import này
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.defty_movie_app.data.dto.DownloadedMovie;

public class DownloadCompletionReceiver extends BroadcastReceiver {

    private static final String TAG = "DownloadReceiver";
    // Định nghĩa một action cho broadcast của bạn
    public static final String ACTION_DOWNLOAD_STATUS_CHANGED = "com.example.defty_movie_app.DOWNLOAD_STATUS_CHANGED";
    public static final String EXTRA_DOWNLOAD_ID = "com.example.defty_movie_app.EXTRA_DOWNLOAD_ID";


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            Bundle extras = intent.getExtras();
            long downloadId = extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            if (downloadId == -1) {
                Log.e(TAG, "Không nhận được Download ID hợp lệ.");
                return;
            }

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) {
                Log.e(TAG, "DownloadManager service không khả dụng.");
                return;
            }
            Cursor cursor = dm.query(query);

            String movieTitleForToast = "Phim"; // Giá trị mặc định

            if (cursor != null && cursor.moveToFirst()) {
                int statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int reasonColumn = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                int titleColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                int localUriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                int status = cursor.getInt(statusColumn);
                String titleFromManager = cursor.getString(titleColumn);
                if (titleFromManager != null) movieTitleForToast = titleFromManager;
                String localUriString = cursor.getString(localUriColumn);

                DownloadStorageManager storageManager = new DownloadStorageManager(context);
                DownloadedMovie movieToUpdate = storageManager.findMovieByDownloadId(downloadId);

                if (movieToUpdate == null) {
                    Log.e(TAG, "Không tìm thấy phim với Download ID: " + downloadId + " trong SharedPreferences.");
                    cursor.close();
                    // Vẫn gửi broadcast để ViewModel có thể dọn dẹp nếu cần
                    sendDownloadStatusBroadcast(context, downloadId);
                    return;
                }
                // Cập nhật title cho toast nếu có từ movieToUpdate (có thể chứa tên tập)
                if (movieToUpdate.getTitle() != null) movieTitleForToast = movieToUpdate.getTitle();


                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    Log.i(TAG, "Tải xuống thành công cho ID: " + downloadId + ", Tiêu đề: " + movieToUpdate.getTitle());
                    if (localUriString != null) {
                        Uri localUri = Uri.parse(localUriString);
                        String filePath = getPathFromUri(context, dm, downloadId); // Sử dụng hàm getPathFromUri
                        if (filePath == null && localUri.getScheme() != null && localUri.getScheme().equals("file")) {
                            filePath = localUri.getPath();
                        }

                        if (filePath != null) {
                            movieToUpdate.setLocalFilePath(filePath);
                            Log.i(TAG, "Đường dẫn file: " + filePath);
                        } else {
                            movieToUpdate.setLocalFilePath(localUriString); // Lưu content URI nếu không lấy được path
                            Log.w(TAG, "Không lấy được file path, lưu content URI: " + localUriString);
                        }
                        movieToUpdate.setDownloadStatus(DownloadedMovie.STATUS_COMPLETED);
                    } else {
                        Log.e(TAG, "localUriString là null cho tải xuống thành công ID: " + downloadId);
                        movieToUpdate.setDownloadStatus(DownloadedMovie.STATUS_FAILED);
                    }
                } else {
                    int reason = cursor.getInt(reasonColumn);
                    Log.e(TAG, "Tải xuống thất bại cho ID: " + downloadId + ", Tiêu đề: " + movieToUpdate.getTitle() + ". Lý do: " + getDownloadErrorReason(reason));
                    movieToUpdate.setDownloadStatus(DownloadedMovie.STATUS_FAILED);
                }
                storageManager.updateDownloadedMovie(movieToUpdate);
                Toast.makeText(context, "Tải " + movieTitleForToast + (status == DownloadManager.STATUS_SUCCESSFUL ? " hoàn tất." : " thất bại."), Toast.LENGTH_LONG).show();

                cursor.close();
            } else {
                Log.e(TAG, "Cursor rỗng hoặc không thể di chuyển đến first cho Download ID: " + downloadId);
                // Có thể một download đã bị hủy nhưng receiver vẫn được gọi
                // Thử tìm và cập nhật trạng thái nếu có trong storage
                DownloadStorageManager storageManager = new DownloadStorageManager(context);
                DownloadedMovie moviePossiblyCancelled = storageManager.findMovieByDownloadId(downloadId);
                if (moviePossiblyCancelled != null && DownloadedMovie.STATUS_DOWNLOADING.equals(moviePossiblyCancelled.getDownloadStatus())) {
                    moviePossiblyCancelled.setDownloadStatus(DownloadedMovie.STATUS_FAILED); // Hoặc một trạng thái "cancelled" nếu có
                    storageManager.updateDownloadedMovie(moviePossiblyCancelled);
                    if (moviePossiblyCancelled.getTitle() != null) movieTitleForToast = moviePossiblyCancelled.getTitle();
                    Toast.makeText(context, "Tải " + movieTitleForToast + " có thể đã bị hủy hoặc lỗi.", Toast.LENGTH_LONG).show();
                }
            }

            // GỬI LOCAL BROADCAST SAU KHI XỬ LÝ
            sendDownloadStatusBroadcast(context, downloadId);
        }
    }

    private void sendDownloadStatusBroadcast(Context context, long downloadId) {
        Intent statusIntent = new Intent(ACTION_DOWNLOAD_STATUS_CHANGED);
        statusIntent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
        LocalBroadcastManager.getInstance(context).sendBroadcast(statusIntent);
        Log.d(TAG, "Sent local broadcast: ACTION_DOWNLOAD_STATUS_CHANGED for downloadId: " + downloadId);
    }


    // ... (getPathFromUri và getDownloadErrorReason giữ nguyên)
    private String getPathFromUri(Context context, DownloadManager dm, long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = null;
        try {
            cursor = dm.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                int localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                if (localUriIndex != -1) {
                    String localUriString = cursor.getString(localUriIndex);
                    if (localUriString != null) {
                        Uri uri = Uri.parse(localUriString);
                        if ("file".equals(uri.getScheme())) {
                            return uri.getPath();
                        }
                    }
                }
                // Cố gắng lấy COLUMN_LOCAL_FILENAME nếu COLUMN_LOCAL_URI không phải là file URI
                int localFilenameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                if (localFilenameIndex != -1) {
                    String path = cursor.getString(localFilenameIndex);
                    if (path != null && !path.isEmpty()) return path;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.w(TAG, "Could not determine file path for downloadId: " + downloadId);
        return null;
    }


    private String getDownloadErrorReason(int reason) {
        String reasonText = "Unknown error (" + reason + ")";
        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME: reasonText = "ERROR_CANNOT_RESUME"; break;
            case DownloadManager.ERROR_DEVICE_NOT_FOUND: reasonText = "ERROR_DEVICE_NOT_FOUND"; break;
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS: reasonText = "ERROR_FILE_ALREADY_EXISTS"; break;
            case DownloadManager.ERROR_FILE_ERROR: reasonText = "ERROR_FILE_ERROR"; break;
            case DownloadManager.ERROR_HTTP_DATA_ERROR: reasonText = "ERROR_HTTP_DATA_ERROR"; break;
            case DownloadManager.ERROR_INSUFFICIENT_SPACE: reasonText = "ERROR_INSUFFICIENT_SPACE"; break;
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS: reasonText = "ERROR_TOO_MANY_REDIRECTS"; break;
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE: reasonText = "ERROR_UNHANDLED_HTTP_CODE"; break;
            case DownloadManager.ERROR_UNKNOWN: reasonText = "ERROR_UNKNOWN"; break;
        }
        return reasonText;
    }
}