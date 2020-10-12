package com.sudhindra.deltaappdevproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.like.LikeButton;
import com.like.OnLikeListener;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.databinding.PostRowBinding;
import com.sudhindra.deltaappdevproject.utils.Post;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static com.thekhaeng.pushdownanim.PushDownAnim.MODE_STATIC_DP;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private ArrayList<Post> posts;
    private PostAdapterListener listener;

    private String currentUserUid;

    public PostAdapter(Context context, String currentUserUid) {
        this.context = context;
        this.currentUserUid = currentUserUid;
    }

    public void setPosts(ArrayList<Post> posts) {
        this.posts = posts;
    }

    public void setListener(PostAdapterListener listener) {
        this.listener = listener;
    }

    public interface PostAdapterListener {
        void onProfileClicked(int pos);

        void onTextRecognitionClicked(ImageView imageView);

        void onShareClicked(int pos, ImageView imageView);

        void onSaveClicked(int pos, ImageView imageView);

        void onLiked(int pos);

        void onUnLiked(int pos);

        void onCommentClicked(int pos);
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {

        private PostRowBinding binding;
        private String currentUserUid;

        public PostViewHolder(@NonNull PostRowBinding postRowBinding, PostAdapterListener listener) {
            super(postRowBinding.getRoot());
            binding = postRowBinding;

            PushDownAnim.setPushDownAnimTo(binding.postProfile).setScale(MODE_STATIC_DP, PushDownAnim.DEFAULT_PUSH_STATIC);
            PushDownAnim.setPushDownAnimTo(binding.postUsername).setScale(MODE_STATIC_DP, PushDownAnim.DEFAULT_PUSH_STATIC);

            binding.postProfile.setOnClickListener(v -> listener.onProfileClicked(getAdapterPosition()));
            binding.postUsername.setOnClickListener(v -> listener.onProfileClicked(getAdapterPosition()));

            binding.postOptions.setOnClickListener(v -> {
                if (posts.get(getAdapterPosition()).isImagePost())
                    showPopupMenuWithIcons(binding.postOptions, R.menu.image_post_options_menu);
                else
                    showPopupMenuWithIcons(binding.postOptions, R.menu.text_post_options_menu);
            });

            binding.postLikeBt.setOnLikeListener(new OnLikeListener() {
                @Override
                public void liked(LikeButton likeButton) {
                    listener.onLiked(getAdapterPosition());
                }

                @Override
                public void unLiked(LikeButton likeButton) {
                    listener.onUnLiked(getAdapterPosition());
                }
            });

            binding.postCommentBt.setOnClickListener(v -> listener.onCommentClicked(getAdapterPosition()));
        }

        private void setDetails(Post post) {
            binding.setCurrentUid(currentUserUid);
            binding.setPost(post);
        }

        public void showPopupMenuWithIcons(View view, int menuResId) {
            PopupMenu popup = new PopupMenu(context, view);
            try {
                Field[] fields = popup.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if ("mPopup".equals(field.getName())) {
                        field.setAccessible(true);
                        Object menuPopupHelper = field.get(popup);
                        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                        Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                        setForceIcons.invoke(menuPopupHelper, true);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            popup.getMenuInflater().inflate(menuResId, popup.getMenu());
            popup.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    case R.id.imagePostTextRecognition:
                        listener.onTextRecognitionClicked(binding.postImg);
                        return true;
                    case R.id.imagePostShare:
                    case R.id.textPostShare:
                        listener.onShareClicked(getAdapterPosition(), binding.postImg);
                        return true;
                    case R.id.imagePostSave:
                        listener.onSaveClicked(getAdapterPosition(), binding.postImg);
                        return true;
                    default:
                        return false;
                }
            });
            popup.show();
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        PostRowBinding binding = PostRowBinding.inflate(inflater, parent, false);
        return new PostViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.currentUserUid = currentUserUid;
        holder.setDetails(posts.get(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
