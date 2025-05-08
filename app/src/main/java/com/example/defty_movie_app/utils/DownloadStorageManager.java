package com.example.defty_movie_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

// Đảm bảo import đúng package của DownloadedMovie
import com.example.defty_movie_app.data.dto.DownloadedMovie;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    public void addDownloadedMovie(DownloadedMovie movie) {
        if (movie == null) {
            Log.e(TAG, "Attempted to add a null movie.");
            return;
        }
        List<DownloadedMovie> movies = getDownloadedMovies();
        LinkedHashSet<DownloadedMovie> movieSet = new LinkedHashSet<>();

        // Kiểm tra xem phim đã tồn tại chưa (dựa trên equals/hashCode: id và slug)
        // Nếu tồn tại, không thêm lại mà cập nhật thông tin (đặc biệt là downloadId và status)
        boolean found = false;
        for (int i = 0; i < movies.size(); i++) {
            if (movies.get(i).equals(movie)) { // equals so sánh id và slug
                // Cập nhật thông tin cho phim đã tồn tại
                movies.set(i, movie);
                found = true;
                break;
            }
        }
        if (!found) {
            movies.add(0, movie); // Thêm phim mới lên đầu danh sách
        }

        saveDownloadedMovies(movies);
        Log.d(TAG, (found ? "Updated" : "Added") + " movie: " + movie.getTitle() + ". Total: " + movies.size());
    }

    // Phương thức mới để cập nhật thông tin phim (ví dụ: sau khi tải xong)
    public void updateDownloadedMovie(DownloadedMovie movieToUpdate) {
        if (movieToUpdate == null) return;
        List<DownloadedMovie> movies = getDownloadedMovies();
        boolean updated = false;
        for (int i = 0; i < movies.size(); i++) {
            // Tìm phim dựa trên downloadId hoặc id và slug
            if (movies.get(i).getDownloadId() == movieToUpdate.getDownloadId() && movieToUpdate.getDownloadId() != -1) {
                movies.set(i, movieToUpdate);
                updated = true;
                break;
            } else if (movies.get(i).getId() == movieToUpdate.getId() && movies.get(i).getSlug().equals(movieToUpdate.getSlug())) {
                // Fallback nếu downloadId không khớp nhưng id và slug khớp
                movies.set(i, movieToUpdate);
                updated = true;
                break;
            }
        }
        if (updated) {
            saveDownloadedMovies(movies);
            Log.d(TAG, "Updated movie info for: " + movieToUpdate.getTitle());
        } else {
            Log.w(TAG, "Could not find movie to update: " + movieToUpdate.getTitle());
            // Optionally add it if not found, though ideally it should exist
            // addDownloadedMovie(movieToUpdate);
        }
    }


    // Phương thức mới để tìm phim bằng downloadId
    public DownloadedMovie findMovieByDownloadId(long downloadId) {
        if (downloadId == -1) return null;
        List<DownloadedMovie> movies = getDownloadedMovies();
        for (DownloadedMovie movie : movies) {
            if (movie.getDownloadId() == downloadId) {
                return movie;
            }
        }
        return null;
    }


    public void removeDownloadedMovie(DownloadedMovie movieToRemove) {
        if (movieToRemove == null) return;
        List<DownloadedMovie> movies = getDownloadedMovies();
        boolean removed = movies.remove(movieToRemove);
        if(removed) {
            saveDownloadedMovies(movies);
            Log.d(TAG, "Removed movie: " + movieToRemove.getTitle() + ". Total: " + movies.size());
        } else {
            Log.d(TAG, "Movie not found for removal: " + movieToRemove.getTitle());
        }
    }

    public void removeDownloadedMovieById(int movieId, String slug) {
        // Implementation không thay đổi
        List<DownloadedMovie> movies = getDownloadedMovies();
        DownloadedMovie movieToRemove = null;
        for (DownloadedMovie movie : movies) {
            if (movie.getId() == movieId && movie.getSlug().equals(slug)) {
                movieToRemove = movie;
                break;
            }
        }
        if (movieToRemove != null) {
            movies.remove(movieToRemove);
            saveDownloadedMovies(movies);
            Log.d(TAG, "Removed movie by ID: " + movieId + ", Slug: " + slug + ". Total: " + movies.size());
        } else {
            Log.d(TAG, "Movie not found for removal by ID: " + movieId + ", Slug: " + slug);
        }
    }


    private void saveDownloadedMovies(List<DownloadedMovie> movies) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String jsonMovies = gson.toJson(movies);
        editor.putString(DOWNLOADED_MOVIES_KEY, jsonMovies);
        editor.apply();
    }

    public void clearAllDownloads() {
        // Implementation không thay đổi
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(DOWNLOADED_MOVIES_KEY);
        editor.apply();
        Log.d(TAG, "Cleared all downloaded movies.");
    }
}
