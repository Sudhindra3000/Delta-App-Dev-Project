package com.sudhindra.deltaappdevproject.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.adapters.CommentAdapter;
import com.sudhindra.deltaappdevproject.clients.ExternalStorageClient;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.clients.TextRecognitionClient;
import com.sudhindra.deltaappdevproject.databinding.ActivityPostBinding;
import com.sudhindra.deltaappdevproject.models.Comment;
import com.sudhindra.deltaappdevproject.models.Post;
import com.sudhindra.deltaappdevproject.utils.ShareUtil;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

import static com.thekhaeng.pushdownanim.PushDownAnim.MODE_STATIC_DP;

@AndroidEntryPoint
public class PostActivity extends AppCompatActivity {

    private static final String TAG = "PostActivity";
    public static final int POST_ACTIVITY_REQUEST = 909;
    public static final int POST_CHANGED = 890, POST_UNCHANGED = 789;
    private final int COMMENTS_LIMIT = 10;
    private ActivityPostBinding binding;

    private Gson gson;

    private TextRecognitionClient textRecognitionClient;

    // User and Post Data
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private int pos, likesNum = 0, commentNum = 0;
    private Post post;
    private String currentUserName;

    // Firestore Data
    private CollectionReference postComments;
    private CollectionReference postsCollection = FirestoreClient.getInstance().getPostsCollection();
    private DocumentSnapshot lastComment = null;

    // UI
    private ArrayList<Comment> comments;
    private CommentAdapter commentAdapter;

    private boolean loading = true;
    private boolean commentsExist, endOfCollection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.postToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        textRecognitionClient = new TextRecognitionClient(this);

        binding.postToolbar.setOnClickListener(v -> binding.postScrollView.fullScroll(NestedScrollView.FOCUS_UP));

        Intent intent = getIntent();
        gson = new Gson();
        pos = intent.getIntExtra("pos", -1);
        post = gson.fromJson(intent.getStringExtra("postJson"), Post.class);
        postComments = FirestoreClient.getInstance().getCommentsForPost(post.getPostedTimeInMillis());
        if (post.getLikes() != null)
            likesNum = post.getLikes().size();
        commentNum = post.getCommentsN();
        currentUserName = intent.getStringExtra("currentUserName");

        binding.addCommentBt.setOnClickListener(v -> {
            if (!binding.commentEt.getText().toString().trim().isEmpty())
                uploadComment();
        });

        buildRecyclerView();

        initPost();
        setPostDetails();

        checkForComments();
    }

    private void initPost() {
        PushDownAnim.setPushDownAnimTo(binding.post.postProfile).setScale(MODE_STATIC_DP, PushDownAnim.DEFAULT_PUSH_STATIC);
        PushDownAnim.setPushDownAnimTo(binding.post.postUsername).setScale(MODE_STATIC_DP, PushDownAnim.DEFAULT_PUSH_STATIC);

        binding.post.postProfile.setOnClickListener(v -> showProfile(post.getUid()));
        binding.post.postUsername.setOnClickListener(v -> showProfile(post.getUid()));

        binding.post.postOptions.setOnClickListener(v -> {
            if (post.isImagePost())
                showPopupMenuWithIcons(binding.post.postOptions, R.menu.image_post_options_menu);
            else
                showPopupMenuWithIcons(binding.post.postOptions, R.menu.text_post_options_menu);
        });

        binding.post.postLikeBt.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                likePost();
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                unLikePost();
            }
        });
        binding.post.postCommentBt.setOnClickListener(v -> {
        });
    }

    private void setPostDetails() {
        binding.post.setCurrentUid(mAuth.getUid());
        binding.post.setPost(post);
    }

    private void buildRecyclerView() {
        binding.commentsRecyclerView.setHasFixedSize(true);
        comments = new ArrayList<>();
        commentAdapter = new CommentAdapter();
        commentAdapter.setComments(comments);
        commentAdapter.setListener(position -> showProfile(comments.get(position).getUid()));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        binding.commentsRecyclerView.setAdapter(commentAdapter);
        binding.commentsRecyclerView.setLayoutManager(layoutManager);

        binding.postScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            View view = binding.postScrollView.getChildAt(binding.postScrollView.getChildCount() - 1);

            int diff = (view.getBottom() - (binding.postScrollView.getHeight() + binding.postScrollView.getScrollY()));

            if (diff == 0 && loading) {
                loading = false;
                paginate();
            }
        });
    }

    private void showProfile(String otherUserId) {
        if (!otherUserId.equals(mAuth.getUid())) {
            Intent intent = new Intent(this, UserActivity.class);
            intent.putExtra("uid", otherUserId);
            intent.putExtra("currentUserName", currentUserName);
            startActivity(intent);
        }
    }

    // Post Actions
    public void showPopupMenuWithIcons(View view, int menuResId) {
        PopupMenu popup = new PopupMenu(this, view);
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
                    processImageForText(binding.post.postImg);
                    return true;
                case R.id.imagePostShare:
                case R.id.textPostShare:
                    sharePost(binding.post.postImg);
                    return true;
                case R.id.imagePostSave:
                    savePost(binding.post.postImg);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void processImageForText(ImageView imageView) {
        textRecognitionClient.processImage(imageView);
    }

    private void sharePost(ImageView imageView) {
        String head, body = post.getPostDescription();
        if (!post.getUid().equals(mAuth.getUid()))
            head = "Checkout this Post by " + post.getUserName() + " in " + getResources().getString(R.string.app_name) + " on " + post.getPostDate() + ":";
        else
            head = "Checkout this Post by me in " + getResources().getString(R.string.app_name) + " on " + post.getPostDate() + ":";
        ShareUtil.sharePost(this, post, head, body, imageView);
    }

    private void savePost(ImageView imageView) {
        ExternalStorageClient.checkWriteExternalStoragePermission(this, imageView, String.valueOf(post.getPostedTimeInMillis()), this);
    }

    private void likePost() {
        if (post.getLikes() == null)
            post.setLikes(new ArrayList<>());
        post.getLikes().add(mAuth.getUid());
        binding.post.postLikesTv.setText(String.valueOf(post.getLikes().size()));
        FirestoreClient.getInstance().getPostsCollection().document(String.valueOf(post.getPostedTimeInMillis()))
                .update(Post.LIKES, FieldValue.arrayUnion(mAuth.getUid()))
                .addOnFailureListener(e -> {
                    post.getLikes().remove(mAuth.getUid());
                    binding.post.postLikesTv.setText(String.valueOf(post.getLikes().size()));
                    Log.i(TAG, "onLiked: " + e.getMessage());
                    Toast.makeText(this, "Failed to Like Post", Toast.LENGTH_SHORT).show();
                });
    }

    private void unLikePost() {
        if (post.getLikes() == null)
            post.setLikes(new ArrayList<>());
        post.getLikes().remove(mAuth.getUid());
        binding.post.postLikesTv.setText(String.valueOf(post.getLikes().size()));
        FirestoreClient.getInstance().getPostsCollection().document(String.valueOf(post.getPostedTimeInMillis()))
                .update(Post.LIKES, FieldValue.arrayRemove(mAuth.getUid()))
                .addOnFailureListener(e -> {
                    post.getLikes().add(mAuth.getUid());
                    binding.post.postLikesTv.setText(String.valueOf(post.getLikes().size()));
                    Log.i(TAG, "onLiked: " + e.getMessage());
                    Toast.makeText(this, "Failed to UnLike Post", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkForComments() {
        commentsExist = (post.getCommentsN() != 0);
        if (commentsExist) {
            binding.commentsRecyclerView.setVisibility(View.VISIBLE);
            loadComments();
            return;
        }
        loading = false;
        binding.commentsPb.setVisibility(View.GONE);
        binding.noCommentsTextview.setVisibility(View.VISIBLE);
    }

    private void paginate() {
        Log.i(TAG, "paginate: ");
        loadComments();
    }

    private void loadComments() {
        if (!endOfCollection) {
            Query query;
            if (lastComment != null)
                query = postComments.orderBy(Comment.COMMENT_TIME_IN_MILLIS, Query.Direction.DESCENDING).startAfter(lastComment).limit(COMMENTS_LIMIT);
            else
                query = postComments.orderBy(Comment.COMMENT_TIME_IN_MILLIS, Query.Direction.DESCENDING).limit(COMMENTS_LIMIT);
            query.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        loading = true;
                        binding.commentsPb.setVisibility(View.GONE);
                        comments.addAll(queryDocumentSnapshots.toObjects(Comment.class));
                        commentAdapter.notifyDataSetChanged();
                        if (queryDocumentSnapshots.size() < COMMENTS_LIMIT) {
                            binding.commentsBottomPb.setVisibility(View.GONE);
                            endOfCollection = true;
                            Log.i(TAG, "loadComments: endOfCollection");
                        } else {
                            lastComment = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                            binding.commentsBottomPb.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.commentsPb.setVisibility(View.GONE);
                        Log.i(TAG, "loadComments: " + e.getMessage());
                        Toast.makeText(this, "Failed to Retrieve Comments", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void uploadComment() {
        binding.newCommentPb.setVisibility(View.VISIBLE);
        if (!commentsExist) {
            commentsExist = true;
            binding.noCommentsTextview.setVisibility(View.GONE);
            binding.commentsRecyclerView.setVisibility(View.VISIBLE);
        }
        String text = binding.commentEt.getText().toString();
        binding.commentEt.setText(null);
        Comment comment = new Comment(mAuth.getUid(), currentUserName, text, System.currentTimeMillis());
        WriteBatch writeBatch = FirebaseFirestore.getInstance().batch();
        writeBatch.set(FirestoreClient.getInstance().getCommentDocRef(post.getPostedTimeInMillis(), comment.getCommentTimeInMillis()), comment);
        writeBatch.update(postsCollection.document(String.valueOf(post.getPostedTimeInMillis())), Post.COMMENTS_N, FieldValue.increment(1));
        writeBatch.commit()
                .addOnSuccessListener(aVoid -> {
                    post.setCommentsN(post.getCommentsN() + 1);
                    binding.post.postCommentsTv.setText(String.valueOf(post.getCommentsN()));
                    binding.newCommentPb.setVisibility(View.GONE);
                    comments.add(0, comment);
                    binding.commentsRecyclerView.setHasFixedSize(false);
                    commentAdapter.notifyItemInserted(0);
                    binding.commentsRecyclerView.setHasFixedSize(true);
                })
                .addOnFailureListener(e -> {
                    comments.remove(comment);
                    binding.newCommentPb.setVisibility(View.GONE);
                    post.setCommentsN(post.getCommentsN() - 1);
                    binding.post.postCommentsTv.setText(String.valueOf(post.getCommentsN()));
                    Log.i(TAG, "uploadComment: " + e.getMessage());
                    Toast.makeText(this, "Failed to Add Comment", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkForChangeInPost() {
        if (post.getLikes() != null && likesNum != post.getLikes().size() || commentNum != post.getCommentsN()) {
            Intent intent = new Intent();
            intent.putExtra("pos", pos);
            intent.putExtra("changedPost", gson.toJson(post));
            setResult(POST_CHANGED, intent);
        } else
            setResult(POST_UNCHANGED);
    }

    @Override
    public void onBackPressed() {
        if (!isTaskRoot()) {
            if (pos != -1)
                checkForChangeInPost();
            super.onBackPressed();
        } else {
            Intent intent = new Intent(PostActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }
}