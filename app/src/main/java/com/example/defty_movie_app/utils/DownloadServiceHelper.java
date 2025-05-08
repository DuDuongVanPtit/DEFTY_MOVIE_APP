package com.example.defty_movie_app.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class DownloadServiceHelper {

    private static final String TAG = "DownloadServiceHelper";

    public static long startDownload(Context context, String videoUrl, String title, String slug) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(context, "URL video không hợp lệ.", Toast.LENGTH_SHORT).show();
            return -1;
        }

        String fileName = slug + ".mp4"; // Đơn giản hóa tên file

        // Thư mục đích cho DownloadManager (ví dụ: Movies/DeftyMovieApp/yourfile.mp4)
        // DownloadManager sẽ tự tạo thư mục "DeftyMovieApp" nếu nó chưa tồn tại trong Environment.DIRECTORY_MOVIES
        String destinationSubPath = "DeftyMovieApp/" + fileName;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(videoUrl));
        request.setTitle(title);
        request.setDescription("Đang tải xuống phim: " + title);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setAllowedOverMetered(true); // Cho phép tải qua mạng di động (tùy chọn)
        request.setAllowedOverRoaming(true); // Cho phép tải khi chuyển vùng (tùy chọn)

        // Chỉ định thư mục và tên file cho DownloadManager
        // DownloadManager sẽ tạo thư mục con "DeftyMovieApp" trong thư mục "Movies" công cộng nếu nó chưa tồn tại.
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, destinationSubPath);

        // Log đường dẫn dự kiến
        File expectedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), destinationSubPath);
        Log.i(TAG, "File sẽ được lưu tại (dự kiến): " + expectedFile.getAbsolutePath());


        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            try {
                long downloadId = downloadManager.enqueue(request);
                Toast.makeText(context, "Bắt đầu tải: " + title, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Đã yêu cầu tải xuống cho: " + title + ", ID: " + downloadId);
                return downloadId;
            } catch (Exception e) {
                // Các lỗi phổ biến ở đây có thể là SecurityException nếu có vấn đề với quyền hoặc đường dẫn đích
                // trên một số thiết bị/phiên bản Android rất cũ hoặc cấu hình đặc biệt.
                Log.e(TAG, "Lỗi khi yêu cầu tải xuống (enqueue): " + e.getMessage(), e);
                Toast.makeText(context, "Lỗi khi bắt đầu tải: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return -1;
            }
        } else {
            Toast.makeText(context, "Không thể khởi tạo Download Manager.", Toast.LENGTH_SHORT).show();
            return -1;
        }
    }
}
