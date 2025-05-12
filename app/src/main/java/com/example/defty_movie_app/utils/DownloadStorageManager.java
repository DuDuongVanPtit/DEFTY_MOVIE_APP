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
            // So sánh dựa trên equals (id và slug)
            if (movies.get(i).equals(movie)) {
                // Nếu phim đã tồn tại, cập nhật thông tin của nó
                // Điều này quan trọng để cập nhật downloadId, status, localFilePath
                movies.set(i, movie);
                found = true;
                Log.d(TAG, "Updated existing movie: " + movie.getTitle());
                break;
            }
        }
        if (!found) {
            movies.add(0, movie); // Thêm phim mới vào đầu danh sách
            Log.d(TAG, "Added new movie: " + movie.getTitle());
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
            // Ưu tiên tìm bằng downloadId nếu có và hợp lệ
            if (movieToUpdate.getDownloadId() != -1 && existingMovie.getDownloadId() == movieToUpdate.getDownloadId()) {
                movies.set(i, movieToUpdate);
                updated = true;
                break;
            }
            // Nếu không khớp downloadId (hoặc downloadId không hợp lệ), thử khớp bằng id và slug
            else if (existingMovie.getId() == movieToUpdate.getId() &&
                    existingMovie.getSlug() != null &&
                    existingMovie.getSlug().equals(movieToUpdate.getSlug())) {
                movies.set(i, movieToUpdate);
                updated = true;
                break;
            }
        }

        if (updated) {
            saveDownloadedMovies(movies);
            Log.d(TAG, "Updated movie info for: " + movieToUpdate.getTitle());
        } else {
            // Nếu không tìm thấy để cập nhật, bạn có thể quyết định thêm mới ở đây nếu muốn
            // addDownloadedMovie(movieToUpdate);
            Log.w(TAG, "Could not find movie to update (will not add as new): " + movieToUpdate.getTitle() + " with ID: " + movieToUpdate.getId() + " and Slug: " + movieToUpdate.getSlug());
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

    public void removeDownloadedMovieById(int movieId, String slug) {
        if (slug == null) return;
        List<DownloadedMovie> movies = getDownloadedMovies();
        DownloadedMovie movieToRemove = null;
        for (DownloadedMovie movie : movies) {
            if (movie.getId() == movieId && slug.equals(movie.getSlug())) {
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
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(DOWNLOADED_MOVIES_KEY);
        editor.apply();
        Log.d(TAG, "Cleared all downloaded movies.");
    }
}
