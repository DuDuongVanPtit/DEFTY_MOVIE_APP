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

import com.example.defty_movie_app.data.dto.DownloadedMovie; // Đảm bảo import đúng package

import java.util.List;

public class DownloadCompletionReceiver extends BroadcastReceiver {

    private static final String TAG = "DownloadReceiver";

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

            if (cursor != null && cursor.moveToFirst()) {
                int statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int reasonColumn = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                int titleColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                int localUriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                int status = cursor.getInt(statusColumn);
                String title = cursor.getString(titleColumn);
                String localUriString = cursor.getString(localUriColumn);

                DownloadStorageManager storageManager = new DownloadStorageManager(context);
                DownloadedMovie movieToUpdate = storageManager.findMovieByDownloadId(downloadId);

                if (movieToUpdate == null) {
                    Log.e(TAG, "Không tìm thấy phim với Download ID: " + downloadId + " trong SharedPreferences.");
                    cursor.close();
                    return;
                }

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    Log.i(TAG, "Tải xuống thành công cho ID: " + downloadId + ", Tiêu đề: " + title);
                    if (localUriString != null) {
                        Uri localUri = Uri.parse(localUriString);
                        // Đối với file trong thư mục public, localUri có thể là content:// URI
                        // Cần chuyển đổi nó thành file path nếu cần, hoặc sử dụng URI trực tiếp nếu trình phát video hỗ trợ.
                        // Cách đơn giản nhất là lấy path từ DownloadManager nếu có thể.
                        String filePath = getPathFromUri(context, dm, downloadId);
                        if (filePath == null && localUri.getScheme() != null && localUri.getScheme().equals("file")) {
                            filePath = localUri.getPath(); // Nếu là file URI
                        }


                        if (filePath != null) {
                            movieToUpdate.setLocalFilePath(filePath);
                            Log.i(TAG, "Đường dẫn file: " + filePath);
                        } else {
                            // Nếu không lấy được file path trực tiếp, lưu content URI
                            // Trình phát video sau này cần hỗ trợ content URI
                            movieToUpdate.setLocalFilePath(localUriString);
                            Log.w(TAG, "Không lấy được file path, lưu content URI: " + localUriString);
                        }
                        movieToUpdate.setDownloadStatus(DownloadedMovie.STATUS_COMPLETED);
                    } else {
                        Log.e(TAG, "localUriString là null cho tải xuống thành công ID: " + downloadId);
                        movieToUpdate.setDownloadStatus(DownloadedMovie.STATUS_FAILED); // Coi như lỗi nếu không có URI
                    }
                } else {
                    int reason = cursor.getInt(reasonColumn);
                    Log.e(TAG, "Tải xuống thất bại cho ID: " + downloadId + ", Tiêu đề: " + title + ". Lý do: " + getDownloadErrorReason(reason));
                    movieToUpdate.setDownloadStatus(DownloadedMovie.STATUS_FAILED);
                }
                storageManager.updateDownloadedMovie(movieToUpdate); // Cần thêm phương thức này
                Toast.makeText(context, "Tải " + title + (status == DownloadManager.STATUS_SUCCESSFUL ? " hoàn tất." : " thất bại."), Toast.LENGTH_LONG).show();

                cursor.close();
            } else {
                Log.e(TAG, "Cursor rỗng hoặc không thể di chuyển đến first cho Download ID: " + downloadId);
            }
        }
    }

    // Helper method to get file path from DownloadManager (works for some cases)
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
                // For API 24+ (Nougat) COLUMN_LOCAL_FILENAME is deprecated.
                // If COLUMN_LOCAL_URI is a content URI, you might need more complex logic
                // or rely on the content URI directly if your player supports it.
                // For files in public directories, this might be tricky.
                // Let's try to get the path from COLUMN_LOCAL_FILENAME if available (for older APIs)
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
