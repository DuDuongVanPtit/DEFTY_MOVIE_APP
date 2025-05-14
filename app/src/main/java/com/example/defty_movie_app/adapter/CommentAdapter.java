package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.model.response.MovieCommentResponse;
// Đảm bảo import EpisodeCommentUserResponse nếu bạn truy cập trực tiếp các trường của nó
// import com.example.defty_movie_app.data.model.comment.EpisodeCommentUserResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<MovieCommentResponse> commentList;
    private Context context;
    private CommentInteractionListener listener;

    public interface CommentInteractionListener {
        void onLikeClicked(MovieCommentResponse comment, int position);
        void onReplyClicked(MovieCommentResponse comment, int position);
        void onViewRepliesClicked(MovieCommentResponse comment, int position);
        // void onUserAvatarClicked(String userId, String userName); // Nếu cần
    }

    public CommentAdapter(Context context, CommentInteractionListener listener) {
        this.context = context;
        this.commentList = new ArrayList<>();
        this.listener = listener;
    }

    public void setComments(List<MovieCommentResponse> newComments) {
        this.commentList.clear();
        if (newComments != null) {
            this.commentList.addAll(newComments);
        }
        notifyDataSetChanged();
    }

    public void addComments(List<MovieCommentResponse> moreComments) {
        if (moreComments != null && !moreComments.isEmpty()) {
            int startPosition = this.commentList.size();
            this.commentList.addAll(moreComments);
            notifyItemRangeInserted(startPosition, moreComments.size());
        }
    }

    public void addCommentToTop(MovieCommentResponse newComment) {
        if (newComment != null) {
            this.commentList.add(0, newComment);
            notifyItemInserted(0);
            // Cân nhắc cuộn lên đầu danh sách nếu cần
            // if (recyclerView != null) recyclerView.scrollToPosition(0);
        }
    }


    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        MovieCommentResponse comment = commentList.get(position);
        holder.bind(comment, listener, context);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imageUserAvatar;
        TextView textUserName, textTimestamp, textContent, textLikeCount, textViewReplies, textReplyLabel;
        ImageButton buttonLike, buttonReply;
        View itemViewRoot;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            itemViewRoot = itemView;
            imageUserAvatar = itemView.findViewById(R.id.image_comment_user_avatar);
            textUserName = itemView.findViewById(R.id.text_comment_user_name);
            textTimestamp = itemView.findViewById(R.id.text_comment_timestamp);
            textContent = itemView.findViewById(R.id.text_comment_content);
            textLikeCount = itemView.findViewById(R.id.text_comment_like_count);
            textViewReplies = itemView.findViewById(R.id.text_view_replies);
            textReplyLabel = itemView.findViewById(R.id.text_comment_reply_label);
            buttonLike = itemView.findViewById(R.id.button_like_comment);
            buttonReply = itemView.findViewById(R.id.button_reply_comment);
        }

        public void bind(final MovieCommentResponse comment,
                         final CommentInteractionListener listener,
                         Context context) {

            // Sử dụng các getter mới từ MovieCommentResponse (đã bao gồm kiểm tra null cho user)
            textUserName.setText(comment.getUserName()); // getUserName() đã xử lý user null và trả về user.getFullName()
            textContent.setText(comment.getContent());
            textTimestamp.setText(formatTimestamp(comment.getCreatedAt(), context)); // Sử dụng getCreatedAt()

            Glide.with(context)
                    .load(comment.getUserAvatar()) // getUserAvatar() đã xử lý user null
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.default_avt) // Đảm bảo drawable này tồn tại
                    .error(R.drawable.default_avt)       // Đảm bảo drawable này tồn tại
                    .into(imageUserAvatar);

            // Xử lý Like Count
            // Dựa trên DTO backend mới, `comment.getReactions()` là List<CommentReactionResponse>
            // và `CommentReactionResponse` có `content`, `createdDate`, `user`.
            // Chúng ta cần làm rõ "like" được thể hiện như thế nào.
            // Nếu mỗi reaction trong list là một "like", thì size() là số like.
            // Nếu backend có trường likeCount riêng thì tốt hơn.
            // Tạm thời, nếu bạn muốn hiển thị số lượng "reactions" (bất kể loại gì):
            if (comment.getReactions() != null) {
                textLikeCount.setText(String.valueOf(comment.getReactions().size()));
            } else {
                textLikeCount.setText("0");
            }
            // TODO: Cập nhật logic cho nút buttonLike và icon của nó dựa trên trạng thái user đã like hay chưa.
            // Hiện tại, nó chỉ gọi listener.
            buttonLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClicked(comment, getAdapterPosition());
                }
            });


            // Xử lý nút Reply
            buttonReply.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReplyClicked(comment, getAdapterPosition());
                }
            });
            textReplyLabel.setOnClickListener(v -> { // Cho phép nhấn vào text "Trả lời"
                if (listener != null) {
                    listener.onReplyClicked(comment, getAdapterPosition());
                }
            });


            // Xử lý hiển thị "Xem X trả lời"
            if (comment.getTotalReply() != null && comment.getTotalReply() > 0) {
                textViewReplies.setText(String.format(Locale.getDefault(), "Xem %d trả lời", comment.getTotalReply()));
                textViewReplies.setVisibility(View.VISIBLE);
                textViewReplies.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onViewRepliesClicked(comment, getAdapterPosition());
                    }
                });
            } else {
                textViewReplies.setVisibility(View.GONE);
            }
        }

        private String formatTimestamp(String dateString, Context context) {
            if (dateString == null || dateString.isEmpty()) {
                return "";
            }
            // Backend dùng java.util.Date, khi serialize qua JSON thường thành timestamp (long)
            // hoặc một chuỗi ISO 8601.
            // Nếu là timestamp long, bạn cần chuyển nó thành Date rồi format.
            // Nếu là chuỗi ISO 8601 (ví dụ: "2024-07-20T10:30:00.123Z"), SimpleDateFormat cần khớp.

            // Thử parse như chuỗi ISO 8601 trước
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            if (dateString.endsWith("Z")) {
                sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                sdfInput.setTimeZone(TimeZone.getTimeZone("UTC"));
            } else if (dateString.contains(".")) { // Kiểm tra millisecond
                // Thử với định dạng có millisecond
                try {
                    // Cố gắng parse với định dạng có milliseconds trước
                    sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
                    Date testDate = sdfInput.parse(dateString); // Thử parse
                    if (testDate == null) throw new ParseException("Failed",0); // Nếu parse lỗi, thử định dạng không có ms
                } catch (ParseException e) {
                    // Nếu lỗi, quay lại định dạng không có milliseconds
                    sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                }
            }


            try {
                Date date = sdfInput.parse(dateString);
                if (date == null) return dateString; // Trả về chuỗi gốc nếu không parse được

                long time = date.getTime();
                long now = System.currentTimeMillis();
                // Điều chỉnh 'now' theo múi giờ của server nếu cần, hoặc đảm bảo server trả về UTC
                // và client hiển thị theo múi giờ địa phương.
                // Hiện tại, giả sử 'time' đã là UTC và 'now' là local.
                // Để so sánh đúng, cả hai nên cùng múi giờ hoặc diff đã tính đến múi giờ.
                // Cách đơn giản là giả sử thời gian từ server là UTC và client hiển thị tương đối.

                long diff = now - time;

                if (diff < 0) { // Thời gian trong tương lai, có thể do lệch múi giờ
                    // Log.w("CommentAdapter", "Timestamp from future? " + dateString);
                    // Trả về một định dạng ngày cụ thể thay vì "vừa xong"
                    SimpleDateFormat sdfOutputSimple = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
                    return sdfOutputSimple.format(date);
                }


                long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                long hours = TimeUnit.MILLISECONDS.toHours(diff);
                long days = TimeUnit.MILLISECONDS.toDays(diff);

                if (seconds < 60) {
                    return seconds <= 1 ? "vừa xong" : seconds + " giây trước";
                } else if (minutes < 60) {
                    return minutes + " phút trước";
                } else if (hours < 24) {
                    return hours + " giờ trước";
                } else if (days < 7) {
                    return days + " ngày trước";
                } else {
                    SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    return sdfOutput.format(date);
                }
            } catch (ParseException e) {
                Log.e("CommentAdapter", "Error parsing date: " + dateString, e);
                // Thử parse như một timestamp long nếu parse chuỗi thất bại
                try {
                    long timeMillis = Long.parseLong(dateString);
                    Date dateFromTimestamp = new Date(timeMillis);
                    long now = System.currentTimeMillis();
                    long diff = now - timeMillis;
                    if (diff < 0) return "vừa xong";
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
                    if (seconds < 60) return seconds <= 1 ? "vừa xong" : seconds + " giây trước";
                    // ... (logic tương tự như trên)
                    return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateFromTimestamp);

                } catch (NumberFormatException nfe) {
                    Log.e("CommentAdapter", "Error parsing date string and not a long timestamp: " + dateString, nfe);
                }
                return dateString; // Trả về chuỗi gốc nếu mọi cách parse đều thất bại
            }
        }
    }
}
