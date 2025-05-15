package com.example.defty_movie_app.data.remote;

import com.example.defty_movie_app.data.model.request.LoginRequest;
import com.example.defty_movie_app.data.model.request.MovieCommentRequest;
import com.example.defty_movie_app.data.model.request.SignUpRequest;
import com.example.defty_movie_app.data.model.response.ApiResponse;
import com.example.defty_movie_app.data.model.response.EpisodeResponse;
import com.example.defty_movie_app.data.model.response.LoginResponse;
import com.example.defty_movie_app.data.model.response.MovieCommentResponse;
import com.example.defty_movie_app.data.model.response.MovieDetailResponse;
import com.example.defty_movie_app.data.model.response.PageableResponse;
import com.example.defty_movie_app.data.model.response.SignUpResponse;
import com.example.defty_movie_app.data.model.response.SimpleResponse;
import com.example.defty_movie_app.data.model.response.UserResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AuthApiService {
    @POST("api/v1/user/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("api/v1/user/auth/register")
    Call<ApiResponse<SignUpResponse>> signUp(@Body SignUpRequest request);

    @GET("api/v1/user/auth/check-account-token")
    Call<ApiResponse<UserResponse>> checkAccount(@Query("token") String token);

    @GET("api/v1/user/accessible/episode/first")
    Call<MovieDetailResponse> getMovieDetail(@Query("slug") String slug);

    @GET("api/v1/user/accessible/episode/first/video")
    Call<EpisodeResponse> getEpisode(@Query("slug") String slug);

    // --- API cho Bình luận Phim ---

    /**
     * Lấy danh sách bình luận cho một tập phim (phân trang).
     * Endpoint: GET /api/user/accessible/movie-comment/{episodeId}?page=0&size=10
     * (Giả sử ${api.prefix} = /api)
     */
    @GET("api/v1/user/accessible/movie-comment/{episodeId}")
    Call<SimpleResponse<PageableResponse<MovieCommentResponse>>> getMovieComments(
            @Path("episodeId") int episodeId,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Thêm một bình luận mới.
     * Endpoint: POST /api/user/movie-comment
     */
    @POST("api/v1/user/movie-comment")
    Call<SimpleResponse> addMovieComment( // Giả sử backend trả về một response đơn giản chứa ID hoặc message
                                          @Body MovieCommentRequest commentRequest
                                          // @Header("Authorization") String authToken // Nếu API yêu cầu token
    );

    /**
     * Cập nhật một bình luận đã có.
     * Endpoint: PATCH /api/user/movie-comment/{commentId}
     */
    @PATCH("api/v1/user/movie-comment/{commentId}")
    Call<SimpleResponse> updateMovieComment(
            @Path("commentId") int commentId,
            @Body MovieCommentRequest commentUpdateRequest // Backend của bạn dùng MovieCommentUpdateRequest, cần tạo DTO này
            // @Header("Authorization") String authToken
    );

    /**
     * Xóa một bình luận.
     * Endpoint: DELETE /api/user/movie-comment/{commentId}
     */
    @DELETE("api/v1/user/movie-comment/{commentId}")
    Call<SimpleResponse> deleteMovieComment(
            @Path("commentId") int commentId
            // @Header("Authorization") String authToken
    );

    /**
     * Lấy danh sách các bình luận trả lời cho một bình luận cha (phân trang).
     * Endpoint: GET /api/user/accessible/movie-comment/{commentId}/replies?page=0&size=5
     */
    @GET("api/v1/user/accessible/movie-comment/{commentId}/replies") // Đảm bảo path đúng, bỏ /api/v1 nếu base URL đã có
    Call<SimpleResponse<List<MovieCommentResponse>>> getMovieCommentReplies(
            @Path("commentId") int parentCommentId
            // API backend của bạn cho replies không có tham số page/size trong path này
            // Nếu backend có hỗ trợ phân trang cho replies, bạn sẽ thêm @Query("page") và @Query("size")
    );

    // --- Kết thúc API cho Bình luận Phim ---
}
