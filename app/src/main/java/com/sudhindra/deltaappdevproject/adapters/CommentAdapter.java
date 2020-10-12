package com.sudhindra.deltaappdevproject.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sudhindra.deltaappdevproject.databinding.CommentItemBinding;
import com.sudhindra.deltaappdevproject.models.Comment;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.ArrayList;

import static com.thekhaeng.pushdownanim.PushDownAnim.MODE_STATIC_DP;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private ArrayList<Comment> comments;

    private CommentAdapterListener listener;

    public interface CommentAdapterListener {
        void onProfileClicked(int pos);
    }

    public void setComments(ArrayList<Comment> comments) {
        this.comments = comments;
    }

    public void setListener(CommentAdapterListener listener) {
        this.listener = listener;
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {

        private CommentItemBinding binding;

        public CommentViewHolder(@NonNull CommentItemBinding commentItemBinding, CommentAdapterListener listener) {
            super(commentItemBinding.getRoot());
            binding = commentItemBinding;

            PushDownAnim.setPushDownAnimTo(binding.commentProfile).setScale(MODE_STATIC_DP, PushDownAnim.DEFAULT_PUSH_STATIC);
            PushDownAnim.setPushDownAnimTo(binding.commentUsername).setScale(MODE_STATIC_DP, PushDownAnim.DEFAULT_PUSH_STATIC);
            binding.commentProfile.setOnClickListener(v -> listener.onProfileClicked(getAdapterPosition()));
            binding.commentUsername.setOnClickListener(v -> listener.onProfileClicked(getAdapterPosition()));
        }

        private void setDetails(Comment comment) {
            binding.setComment(comment);
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        CommentItemBinding binding = CommentItemBinding.inflate(inflater, parent, false);
        return new CommentViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.setDetails(comments.get(position));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }
}
