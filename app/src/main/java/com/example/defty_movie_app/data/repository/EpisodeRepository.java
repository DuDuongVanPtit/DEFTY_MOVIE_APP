package com.example.defty_movie_app.data.repository;

import com.example.defty_movie_app.data.remote.EpisodeUserService;
import retrofit2.Retrofit;

/**
 * Repository để xử lý các thao tác dữ liệu liên quan đến Episode.
 * Triển khai mẫu Singleton để đảm bảo chỉ có một instance duy nhất trong ứng dụng.
 */
public class EpisodeRepository {

    // Instance duy nhất của Repository
    private static EpisodeRepository instance;

    // Instance của EpisodeUserService
    private final EpisodeUserService api;

    /**
     * Constructor private để ngăn chặn việc tạo instance trực tiếp từ bên ngoài.
     */
    private EpisodeRepository() {
        // Khởi tạo Retrofit và tạo instance của API service
        Retrofit retrofit = ApiClient.getRetrofitInstance(); // Lấy instance Retrofit
        api = retrofit.create(EpisodeUserService.class);
    }

    /**
     * Trả về instance duy nhất của EpisodeRepository.
     * Sử dụng double-checked locking để đảm bảo an toàn luồng khi khởi tạo lười biếng.
     * @return Instance duy nhất của EpisodeRepository.
     */
    public static synchronized EpisodeRepository getInstance() {
        // Sử dụng double-checked locking
        if (instance == null) {
            synchronized (EpisodeRepository.class) {
                if (instance == null) {
                    instance = new EpisodeRepository();
                }
            }
        }
        return instance;
    }

    /**
     * Cung cấp quyền truy cập vào EpisodeUserService.
     * @return Instance của EpisodeUserService.
     */
    public EpisodeUserService getApi() {
        return api;
    }
}