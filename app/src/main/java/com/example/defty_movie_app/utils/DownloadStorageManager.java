package com.example.defty_movie_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.defty_movie_app.data.dto.DownloadedMovie;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet; // Giữ lại nếu bạn muốn đảm bảo thứ tự và tính duy nhất
import java.util.List;

public class DownloadStorageManager {
    private static final String PREFS_NAME = "DownloadedMoviesPrefs";
    private static final String DOWNLOADED_MOVIES_KEY = "downloaded_movies";
    private static final String TAG = "DownloadStorageManager";
    private SharedPreferences sharedPreferences;
    private Gson gson;
    public DownloadStorageManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    //TODO: Lấy thông tin phim dưới sharedPreferences
    public List<DownloadedMovie> getDownloadedMovies() {
        String jsonMovies = sharedPreferences.getString(DOWNLOADED_MOVIES_KEY, null);
        if (jsonMovies == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<DownloadedMovie>>() {}.getType();
        List<DownloadedMovie> movies = gson.fromJson(jsonMovies, type);
        Log.d(TAG, "Loaded " + (movies != null ? movies.size() : 0) + " downloaded movies.");
        return movies != null ? movies : new ArrayList<>();
    }

    //TODO: Save thông tin phim dưới sharedPreferences
    public void addDownloadedMovie(DownloadedMovie movie) {
        if (movie == null) {
            Log.e(TAG, "Attempted to add a null movie.");
            return;
        }
        List<DownloadedMovie> movies = getDownloadedMovies();
        boolean found = false;
        for (int i = 0; i < movies.size(); i++) {
            // Sử dụng equals đã được override trong DownloadedMovie (id, slug, number)
            if (movies.get(i).equals(movie)) {
                // Nếu phim đã tồn tại, cập nhật thông tin của nó
                DownloadedMovie existingMovie = movies.get(i);
                // Giữ lại localFilePath nếu phim mới không có (ví dụ cập nhật trạng thái mà không phải tải lại)
                if (movie.getLocalFilePath() == null && DownloadedMovie.STATUS_COMPLETED.equals(existingMovie.getDownloadStatus())) {
                    movie.setLocalFilePath(existingMovie.getLocalFilePath());
                }
                // Giữ lại downloadId nếu phim mới không có (ví dụ cập nhật trạng thái từ receiver)
                if (movie.getDownloadId() == -1 && existingMovie.getDownloadId() != -1 &&
                        !DownloadedMovie.STATUS_PENDING.equals(movie.getDownloadStatus())) { // Không ghi đè nếu là PENDING mới
                    movie.setDownloadId(existingMovie.getDownloadId());
                }

                movies.set(i, movie);
                found = true;
                Log.d(TAG, "Updated existing movie: " + movie.getTitle() + " with status: " + movie.getDownloadStatus());
                break;
            }
        }
        if (!found) {
            // Đảm bảo tiêu đề có số tập nếu là phim mới
            if (movie.getNumber() != null && !movie.getTitle().endsWith("-Tập " + movie.getNumber())) {
                movie.setTitle(movie.getTitle() + "-Tập " + movie.getNumber());
            }
            movies.add(0, movie); // Thêm phim mới vào đầu danh sách
            Log.d(TAG, "Added new movie: " + movie.getTitle() + " with status: " + movie.getDownloadStatus());
        }
        saveDownloadedMovies(movies);
        Log.d(TAG, "Total movies after add/update: " + movies.size());
    }

    public void updateDownloadedMovie(DownloadedMovie movieToUpdate) {
        if (movieToUpdate == null) {
            Log.e(TAG, "Attempted to update with a null movie.");
            return;
        }
        List<DownloadedMovie> movies = getDownloadedMovies();
        boolean updated = false;
        for (int i = 0; i < movies.size(); i++) {
            DownloadedMovie existingMovie = movies.get(i);
            // Ưu tiên tìm bằng downloadId nếu có và hợp lệ (thường từ DownloadCompletionReceiver)
            if (movieToUpdate.getDownloadId() != -1 && existingMovie.getDownloadId() == movieToUpdate.getDownloadId()) {
                // Khi cập nhật bằng downloadId, đảm bảo tên và số tập được giữ nguyên từ bản ghi cũ nếu bản cập nhật không có
                if(movieToUpdate.getTitle() == null) movieToUpdate.setTitle(existingMovie.getTitle());
                if(movieToUpdate.getNumber() == null) movieToUpdate.setNumber(existingMovie.getNumber());

                movies.set(i, movieToUpdate);
                updated = true;
                Log.d(TAG, "Updated movie by downloadId: " + movieToUpdate.getTitle());
                break;
            }
            // Nếu không khớp downloadId, thử khớp bằng equals (id, slug, number)
            // Điều này hữu ích khi cập nhật trạng thái trước khi có downloadId (VD: từ PENDING sang DOWNLOADING)
            else if (existingMovie.equals(movieToUpdate)) {
                // Khi cập nhật bằng equals, đảm bảo tên và số tập được giữ nguyên từ bản ghi cũ nếu bản cập nhật không có
                if(movieToUpdate.getTitle() == null) movieToUpdate.setTitle(existingMovie.getTitle());
                if(movieToUpdate.getNumber() == null) movieToUpdate.setNumber(existingMovie.getNumber());

                movies.set(i, movieToUpdate);
                updated = true;
                Log.d(TAG, "Updated movie by equals: " + movieToUpdate.getTitle());
                break;
            }
        }

        if (updated) {
            saveDownloadedMovies(movies);
        } else {
            // Nếu không tìm thấy để cập nhật, có thể thêm mới nếu logic yêu cầu
            // Tuy nhiên, updateDownloadedMovie thường được gọi cho phim đã có trong danh sách
            Log.w(TAG, "Could not find movie to update (not adding as new): " + movieToUpdate.getTitle() + " with Download ID: " + movieToUpdate.getDownloadId() + ", Movie ID: " + movieToUpdate.getId() + ", Slug: " + movieToUpdate.getSlug());
            // addDownloadedMovie(movieToUpdate); // Cân nhắc nếu muốn thêm mới tại đây
        }
    }

    public DownloadedMovie findMovieByDownloadId(long downloadId) {
        if (downloadId == -1) return null;
        List<DownloadedMovie> movies = getDownloadedMovies();
        for (DownloadedMovie movie : movies) {
            if (movie.getDownloadId() == downloadId) {
                return movie;
            }
        }
        Log.d(TAG, "Movie not found by downloadId: " + downloadId);
        return null;
    }
    public DownloadedMovie findMovieByIdAndSlug(int movieId, String slug) {
        if (slug == null) {
            Log.w(TAG, "Attempted to find movie with null slug for ID: " + movieId);
            return null;
        }
        List<DownloadedMovie> movies = getDownloadedMovies();
        for (DownloadedMovie movie : movies) {
            if (movie.getId() == movieId && slug.equals(movie.getSlug())) {
                return movie;
            }
        }
        Log.d(TAG, "Movie not found by ID: " + movieId + " and Slug: " + slug);
        return null;
    }
    public void removeDownloadedMovie(DownloadedMovie movieToRemove) {
        if (movieToRemove == null) return;
        List<DownloadedMovie> movies = getDownloadedMovies();
        boolean removed = movies.remove(movieToRemove); // Dựa trên equals/hashCode của DownloadedMovie
        if(removed) {
            saveDownloadedMovies(movies);
            Log.d(TAG, "Removed movie: " + movieToRemove.getTitle() + ". Total: " + movies.size());
        } else {
            Log.d(TAG, "Movie not found for removal: " + movieToRemove.getTitle());
        }
    }


    private void saveDownloadedMovies(List<DownloadedMovie> movies) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String jsonMovies = gson.toJson(movies);
        editor.putString(DOWNLOADED_MOVIES_KEY, jsonMovies);
        editor.apply();
    }

}
