package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText; // Không còn dùng EditText trong ViewHolder này
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout; // Không còn dùng LinearLayout replyInputContainer trong ViewHolder
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.model.response.MovieCommentResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class CommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_PARENT_COMMENT = 1;
    private static final int VIEW_TYPE_REPLY_COMMENT = 2;
    private static final String TAG = "CommentAdapter";

    private List<MovieCommentResponse> displayItems;
    private CommentInteractionListener listener;
    private Context context;

    private Set<Integer> expandedParentCommentIds = new HashSet<>();
    private Map<Integer, List<MovieCommentResponse>> fetchedRepliesMap = new HashMap<>();
    private Set<Integer> loadingRepliesParentIds = new HashSet<>();
    // Không còn replyingToPosition vì input box đã được quản lý bởi Activity

    public CommentAdapter(Context context, CommentInteractionListener listener) {
        this.context = context;
        this.displayItems = new ArrayList<>();
        this.listener = listener;
    }

    public void setComments(List<MovieCommentResponse> comments) {
        this.displayItems.clear();
        if (comments != null) {
            for (MovieCommentResponse comment : comments) {
                if (comment.getParentCommentId() == null) {
                    this.displayItems.add(comment);
                }
            }
        }
        expandedParentCommentIds.clear();
        fetchedRepliesMap.clear();
        notifyDataSetChanged();
    }

    public void addComments(List<MovieCommentResponse> comments) {
        if (comments == null || comments.isEmpty()) return;
        int startPosition = this.displayItems.size();
        int parentCommentAddedCount = 0;
        for (MovieCommentResponse comment : comments) {
            if (comment.getParentCommentId() == null) {
                this.displayItems.add(comment);
                parentCommentAddedCount++;
            }
        }
        if (parentCommentAddedCount > 0) {
            notifyItemRangeInserted(startPosition, parentCommentAddedCount);
        }
    }

    // Trong CommentAdapter.java
    public MovieCommentResponse getCommentAtPosition(int position) {
        if (position >= 0 && position < displayItems.size()) {
            return displayItems.get(position);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= displayItems.size()) {
            return VIEW_TYPE_PARENT_COMMENT; // Default or error view type
        }
        MovieCommentResponse comment = displayItems.get(position);
        if (comment.getParentCommentId() == null) {
            return VIEW_TYPE_PARENT_COMMENT;
        } else {
            return VIEW_TYPE_REPLY_COMMENT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MovieCommentResponse comment = displayItems.get(position);
        CommentViewHolder commentHolder = (CommentViewHolder) holder;

        commentHolder.textViewUserName.setText(comment.getUserName());
        commentHolder.textViewCommentTimestamp.setText(formatRelativeTime(comment.getCreatedAt()));
        commentHolder.textViewCommentContent.setText(comment.getContent());

        Glide.with(context)
                .load(comment.getUserAvatar())
                .apply(new RequestOptions().circleCrop())
                .placeholder(R.drawable.default_avt)
                .error(R.drawable.default_avt)
                .into(commentHolder.imageCommentUserAvatar);

        commentHolder.textViewCommentLikeCount.setText(String.valueOf(comment.getReactions() != null ? comment.getReactions().size() : 0)); // Ví dụ: hiển thị số reactions làm like count
        commentHolder.buttonLikeComment.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                listener.onLikeClicked(displayItems.get(currentPosition), currentPosition);
            }
        });

        if (getItemViewType(position) == VIEW_TYPE_PARENT_COMMENT) {
            ViewGroup.MarginLayoutParams itemParams = (ViewGroup.MarginLayoutParams) commentHolder.itemView.getLayoutParams();
            itemParams.leftMargin = 0;
            commentHolder.itemView.setLayoutParams(itemParams);

            if (loadingRepliesParentIds.contains(comment.getId())) {
                commentHolder.textViewViewReplies.setVisibility(View.GONE);
                commentHolder.progressBarLoadingReplies.setVisibility(View.VISIBLE);
            } else {
                commentHolder.progressBarLoadingReplies.setVisibility(View.GONE);
                if (comment.getTotalReply() > 0) {
                    commentHolder.textViewViewReplies.setVisibility(View.VISIBLE);
                    commentHolder.textViewViewReplies.setText(
                            expandedParentCommentIds.contains(comment.getId()) ? "Ẩn trả lời" : "Xem " + comment.getTotalReply() + " trả lời"
                    );
                } else {
                    commentHolder.textViewViewReplies.setVisibility(View.GONE);
                }
            }
            commentHolder.textViewViewReplies.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && !loadingRepliesParentIds.contains(comment.getId())) {
                    listener.onViewRepliesClicked(displayItems.get(currentPosition), currentPosition);
                }
            });

            commentHolder.buttonReplyComment.setVisibility(View.VISIBLE);
            commentHolder.buttonReplyComment.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    listener.onReplyClicked(displayItems.get(currentPosition), currentPosition, commentHolder.itemView);
                }
            });

        } else { // VIEW_TYPE_REPLY_COMMENT
            ViewGroup.MarginLayoutParams itemParams = (ViewGroup.MarginLayoutParams) commentHolder.itemView.getLayoutParams();
            itemParams.leftMargin = (int) (48 * context.getResources().getDisplayMetrics().density); // Thụt lề 48dp
            commentHolder.itemView.setLayoutParams(itemParams);

            commentHolder.textViewViewReplies.setVisibility(View.GONE);
            commentHolder.progressBarLoadingReplies.setVisibility(View.GONE);
            commentHolder.buttonReplyComment.setVisibility(View.VISIBLE); // Hoặc GONE tùy theo UX bạn muốn

            commentHolder.buttonReplyComment.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    listener.onReplyClicked(displayItems.get(currentPosition), currentPosition, commentHolder.itemView);
                }
            });
        }
    }

    public void setLoadingRepliesState(Integer parentId, boolean isLoading) {
        if (parentId == null) return;
        boolean changed = false;
        if (isLoading) {
            changed = loadingRepliesParentIds.add(parentId);
        } else {
            changed = loadingRepliesParentIds.remove(parentId);
        }

        if (changed) {
            int position = findPositionById(parentId);
            if (position != -1) {
                notifyItemChanged(position);
            }
        }
    }
    public boolean isLoadingReplies(Integer parentId) {
        if (parentId == null) {
            return false;
        }
        return loadingRepliesParentIds.contains(parentId);
    }

    @Override
    public int getItemCount() {
        return displayItems == null ? 0 : displayItems.size();
    }

    public boolean isRepliesExpanded(Integer parentId) {
        return expandedParentCommentIds.contains(parentId);
    }

    public List<MovieCommentResponse> getCachedReplies(Integer parentId) {
        return fetchedRepliesMap.get(parentId);
    }

    public void markAsExpandedWithNoReplies(Integer parentId) {
        if (parentId == null) return;
        fetchedRepliesMap.put(parentId, new ArrayList<>());
        expandedParentCommentIds.add(parentId);
        int parentPosition = findPositionById(parentId);
        if (parentPosition != -1 && parentPosition < getItemCount()) {
            notifyItemChanged(parentPosition);
        }
    }

    public void collapseReplies(Integer parentId, int parentPositionInAdapter) {
        if (parentId == null || !expandedParentCommentIds.contains(parentId) || parentPositionInAdapter == -1 || parentPositionInAdapter >= displayItems.size()) return;

        List<MovieCommentResponse> repliesToRemove = new ArrayList<>();
        int i = parentPositionInAdapter + 1;
        while (i < displayItems.size()) {
            MovieCommentResponse currentItem = displayItems.get(i);
            if (currentItem.getParentCommentId() != null && currentItem.getParentCommentId().equals(parentId)) {
                repliesToRemove.add(currentItem);
            } else {
                break;
            }
            i++;
        }

        if (!repliesToRemove.isEmpty()) {
            displayItems.removeAll(repliesToRemove);
            notifyItemRangeRemoved(parentPositionInAdapter + 1, repliesToRemove.size());
        }
        expandedParentCommentIds.remove(parentId);
        if (parentPositionInAdapter < getItemCount()) {
            notifyItemChanged(parentPositionInAdapter);
        }
    }

    public void expandReplies(Integer parentId, List<MovieCommentResponse> replies, int parentPositionInAdapter) {
        if (parentId == null || expandedParentCommentIds.contains(parentId) || parentPositionInAdapter == -1 || replies == null || replies.isEmpty()) return;
        if (parentPositionInAdapter >= displayItems.size() || !displayItems.get(parentPositionInAdapter).getId().equals(parentId)) {
            Log.w(TAG, "expandReplies: Mismatch or invalid parent position. ParentId: " + parentId + " at adapterPos: " + parentPositionInAdapter);
            // Attempt to find correct position again
            parentPositionInAdapter = findPositionById(parentId);
            if (parentPositionInAdapter == -1) {
                Log.e(TAG, "expandReplies: Parent comment not found in displayItems. Cannot expand.");
                return;
            }
        }


        // Ensure no duplicate expansion if called multiple times rapidly before state updates
        // Check if replies for this parent are already in displayItems
        int checkIndex = parentPositionInAdapter + 1;
        if (checkIndex < displayItems.size() && displayItems.get(checkIndex).getParentCommentId() != null && displayItems.get(checkIndex).getParentCommentId().equals(parentId)) {
            Log.d(TAG, "Replies for parentId " + parentId + " seem to be already expanded. Skipping addAll.");
            // Update state if necessary and notify
            expandedParentCommentIds.add(parentId);
            if (parentPositionInAdapter < getItemCount()) notifyItemChanged(parentPositionInAdapter);
            return;
        }


        displayItems.addAll(parentPositionInAdapter + 1, replies);
        expandedParentCommentIds.add(parentId);
        // fetchedRepliesMap.put(parentId, replies); // Cache is usually done in cacheAndExpandReplies

        notifyItemRangeInserted(parentPositionInAdapter + 1, replies.size());
        if (parentPositionInAdapter < getItemCount()) {
            notifyItemChanged(parentPositionInAdapter);
        }
    }

    public void cacheAndExpandReplies(Integer parentId, List<MovieCommentResponse> replies, int parentPositionInAdapterIfKnown) {
        if (parentId == null) {
            Log.e(TAG, "cacheAndExpandReplies: parentId is null.");
            return;
        }
        fetchedRepliesMap.put(parentId, replies != null ? replies : new ArrayList<>());

        // Always find the current position of the parent in case the list changed
        int currentParentPosition = findPositionById(parentId);
        if (currentParentPosition == -1) {
            Log.w(TAG, "cacheAndExpandReplies: Parent comment with ID " + parentId + " not found. Cannot expand.");
            return;
        }

        if (replies == null || replies.isEmpty()) {
            expandedParentCommentIds.add(parentId);
            notifyItemChanged(currentParentPosition);
            // Toast.makeText(context, "Không có trả lời nào.", Toast.LENGTH_SHORT).show(); // Activity có thể xử lý Toast này
        } else {
            expandReplies(parentId, replies, currentParentPosition);
        }
    }

    public int findPositionById(Integer commentId) {
        if (commentId == null) return -1;
        for (int i = 0; i < displayItems.size(); i++) {
            if (displayItems.get(i).getId().equals(commentId)) {
                return i;
            }
        }
        return -1;
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCommentUserAvatar;
        TextView textViewUserName, textViewCommentContent, textViewCommentTimestamp;
        ImageButton buttonLikeComment, buttonReplyComment;
        TextView textViewCommentLikeCount, textViewCommentReplyLabel;
        TextView textViewViewReplies;
        ProgressBar progressBarLoadingReplies;

        // Không còn các view cho input box riêng trong item
        // LinearLayout replyInputContainer;
        // ImageView imageCurrentUserAvatarReplyInput;
        // EditText editTextReplyInput;
        // ImageButton buttonSendReply;


        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCommentUserAvatar = itemView.findViewById(R.id.image_comment_user_avatar);
            textViewUserName = itemView.findViewById(R.id.text_comment_user_name);
            textViewCommentTimestamp = itemView.findViewById(R.id.text_comment_timestamp);
            textViewCommentContent = itemView.findViewById(R.id.text_comment_content);

            buttonLikeComment = itemView.findViewById(R.id.button_like_comment);
            textViewCommentLikeCount = itemView.findViewById(R.id.text_comment_like_count);

            buttonReplyComment = itemView.findViewById(R.id.button_reply_comment);
            textViewCommentReplyLabel = itemView.findViewById(R.id.text_comment_reply_label);
            textViewViewReplies = itemView.findViewById(R.id.text_view_replies);
            progressBarLoadingReplies = itemView.findViewById(R.id.progress_bar_loading_replies);

            // Không cần findViewById cho các view của reply input đã bị xóa khỏi item_comment.xml
        }
    }

    public interface CommentInteractionListener {
        void onLikeClicked(MovieCommentResponse comment, int position);
        void onReplyClicked(MovieCommentResponse comment, int position, View itemView); // itemView có thể không cần thiết nữa
        void onViewRepliesClicked(MovieCommentResponse parentComment, int position);
        // onSendReplyClicked không còn cần thiết trong interface này nếu việc gửi do Activity xử lý
    }

    private String formatRelativeTime(String isoDateString) {
        if (isoDateString == null) return "";
        try {
            SimpleDateFormat sdfInput;
            // Thử các định dạng phổ biến mà API có thể trả về
            if (isoDateString.endsWith("Z")) { // UTC
                if (isoDateString.contains(".")) { // Có mili giây
                    sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                } else { // Không có mili giây
                    sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                }
                sdfInput.setTimeZone(TimeZone.getTimeZone("UTC"));
            } else if (isoDateString.matches(".*[+-]\\d{2}:\\d{2}$")) { // Có offset dạng +07:00
                // Định dạng này phức tạp với SimpleDateFormat, đặc biệt là dấu ':' trong offset.
                // Ta có thể thử loại bỏ dấu ':' trong offset để SimpleDateFormat hiểu (HHmm)
                String tempDateString = isoDateString;
                if (isoDateString.charAt(isoDateString.length() - 3) == ':') {
                    tempDateString = isoDateString.substring(0, isoDateString.length() - 3) + isoDateString.substring(isoDateString.length() - 2);
                }
                if (tempDateString.contains(".")) {
                    sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
                } else {
                    sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
                }
                isoDateString = tempDateString; // Sử dụng chuỗi đã sửa đổi cho việc parse
            } else { // Không có Z hoặc offset, giả sử local time hoặc cần định dạng cụ thể
                if (isoDateString.contains(".")) {
                    sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
                } else {
                    sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                }
                // Nếu bạn biết nó là UTC nhưng không có 'Z', hãy đặt TimeZone
                // sdfInput.setTimeZone(TimeZone.getTimeZone("UTC"));
            }

            Date date = sdfInput.parse(isoDateString);
            if (date == null) return isoDateString;

            long now = System.currentTimeMillis();
            long diff = now - date.getTime();

            if (diff < 0) { // Thời gian trong tương lai, có thể do chênh lệch múi giờ client/server
                // return "Ngay bây giờ"; // Hoặc hiển thị ngày tháng đầy đủ
                SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return sdfOutput.format(date);
            }

            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            long days = TimeUnit.MILLISECONDS.toDays(diff);

            if (days > 0) {
                if (days == 1) return "Hôm qua";
                if (days < 7) return days + " ngày trước";
                SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return sdfOutput.format(date);
            } else if (hours > 0) {
                return hours + " giờ trước";
            } else if (minutes > 0) {
                return minutes + " phút trước";
            } else if (seconds > 5) {
                return seconds + " giây trước";
            } else {
                return "Vừa xong";
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + isoDateString, e);
            try {
                if (isoDateString.length() >= 10) return isoDateString.substring(0, 10); // "yyyy-MM-dd"
            } catch (Exception ex) {
                // Log.e(TAG, "Error substringing date: " + isoDateString, ex);
            }
            return isoDateString;
        } catch (Exception e) {
            Log.e(TAG, "Generic error formatting time: " + isoDateString, e);
            return isoDateString;
        }
    }
}