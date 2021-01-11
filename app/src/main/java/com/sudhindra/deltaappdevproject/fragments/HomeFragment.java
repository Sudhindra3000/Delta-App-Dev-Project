package com.sudhindra.deltaappdevproject.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.iid.FirebaseInstanceId;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.activities.NewPostActivity;
import com.sudhindra.deltaappdevproject.activities.PostActivity;
import com.sudhindra.deltaappdevproject.activities.UserActivity;
import com.sudhindra.deltaappdevproject.adapters.PostAdapter;
import com.sudhindra.deltaappdevproject.clients.ExternalStorageClient;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.databinding.FragmentHomeBinding;
import com.sudhindra.deltaappdevproject.models.Post;
import com.sudhindra.deltaappdevproject.models.Student;
import com.sudhindra.deltaappdevproject.utils.GsonUtil;
import com.sudhindra.deltaappdevproject.utils.ShareUtil;
import com.sudhindra.deltaappdevproject.utils.TextRecognitionUtil;

import java.util.ArrayList;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    public static final int NEW_POST_REQUEST = 567;
    private int homeFeedLimit = 5;
    private FragmentHomeBinding binding;

    // Data
    private FirebaseAuth mAuth;
    private Student student;
    private DocumentSnapshot lastPost = null;

    private boolean firstSetOfPosts = true, endOfCollection = false, loading = true, refreshing = false;

    // UI
    private ArrayList<Post> posts;
    private PostAdapter postAdapter;

    private ViewTreeObserver.OnScrollChangedListener scrollChangedListener = () -> {
        View view = binding.homeFeedScrollView.getChildAt(binding.homeFeedScrollView.getChildCount() - 1);

        int diff = (view.getBottom() - (binding.homeFeedScrollView.getHeight() + binding.homeFeedScrollView.getScrollY()));

        if (diff == 0 && loading && !refreshing) {
            loading = false;
            Log.i(TAG, "onScrolled: LastItem");
            paginate();
        }
    };

    private PostAdapter.PostAdapterListener postAdapterListener = new PostAdapter.PostAdapterListener() {

        @Override
        public void onProfileClicked(int pos) {
            showProfile(pos);
        }

        @Override
        public void onTextRecognitionClicked(ImageView imageView) {
            processImageForText(imageView);
        }

        @Override
        public void onShareClicked(int pos, ImageView imageView) {
            sharePost(pos, imageView);
        }

        @Override
        public void onSaveClicked(int pos, ImageView imageView) {
            savePost(pos, imageView);
        }

        @Override
        public void onLiked(int pos) {
            likePost(pos);
        }

        @Override
        public void onUnLiked(int pos) {
            unLikePost(pos);
        }

        @Override
        public void onCommentClicked(int pos) {
            showCommentsSections(pos);
        }
    };

    private HomeFragmentListener listener;

    public void setListener(HomeFragmentListener listener) {
        this.listener = listener;
    }

    public interface HomeFragmentListener {
        void onNewPost(Post post);
    }

    public void onReselected() {
        binding.homeFeedScrollView.smoothScrollTo(0, 0);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(firebaseAuth -> mAuth = firebaseAuth);
        FirestoreClient.getInstance().getUserInfoDocRef(mAuth.getUid()).addSnapshotListener(((documentSnapshot, e) -> {
            if (e != null)
                Log.i(TAG, "onCreate: " + e.getMessage());
            if (documentSnapshot != null) {
                if (mAuth.getCurrentUser() != null) {
                    student = documentSnapshot.toObject(Student.class);
                    updateUI();
                }
            }
        }));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.homeToolbar.setOnClickListener(v -> onReselected());
        binding.newPostFAB.setOnClickListener(v -> openNewPostActivity());

        binding.homeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        binding.homeRefreshLayout.setOnRefreshListener(this::refresh);

        buildRecyclerView();

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnSuccessListener(instanceIdResult -> {
                    Log.i(TAG, "onViewCreated: token = " + instanceIdResult.getToken());
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "onViewCreated: " + e.getMessage());
                });
    }

    private void buildRecyclerView() {
        binding.homeFeed.setHasFixedSize(true);
        // To remove the Default Dim animation when onItemChanged() is called
        ((SimpleItemAnimator) Objects.requireNonNull(binding.homeFeed.getItemAnimator())).setSupportsChangeAnimations(false);
        posts = new ArrayList<>();
        postAdapter = new PostAdapter(requireContext(), mAuth.getUid());
        postAdapter.setPosts(posts);
        binding.homeFeed.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.homeFeed.setAdapter(postAdapter);

        postAdapter.setListener(postAdapterListener);

        binding.homeFeedScrollView.getViewTreeObserver().addOnScrollChangedListener(scrollChangedListener);
    }

    private void refresh() {
        Log.i(TAG, "refresh: ");
        refreshing = true;
        endOfCollection = false;
        lastPost = null;
        firstSetOfPosts = true;
        posts.clear();
        postAdapter = new PostAdapter(requireContext(), mAuth.getUid());
        postAdapter.setPosts(posts);
        postAdapter.setListener(postAdapterListener);
        getPosts();
    }

    private void updateUI() {
        getPosts();
    }

    // Post Actions
    private void showProfile(int pos) {
        Intent intent = new Intent(requireActivity(), UserActivity.class);
        intent.putExtra("uid", posts.get(pos).getUid());
        intent.putExtra("currentUserName", student.getFullName());
        startActivity(intent);
    }

    private void processImageForText(ImageView imageView) {
        TextRecognitionUtil.processImage(this, imageView);
    }

    private void sharePost(int pos, ImageView imageView) {
        Post post = posts.get(pos);
        String head = "Checkout this Post by " + post.getUserName() + " in " + getResources().getString(R.string.app_name) + " on " + post.getPostDate() + ":", body = post.getPostDescription();
        ShareUtil.sharePost(this, post, head, body, imageView);
    }

    private void savePost(int pos, ImageView imageView) {
        ExternalStorageClient.checkWriteExternalStoragePermission(requireActivity(), imageView, String.valueOf(posts.get(pos).getPostedTimeInMillis()), requireContext());
    }

    private void likePost(int pos) {
        Post post = posts.get(pos);
        if (post.getLikes() == null)
            post.setLikes(new ArrayList<>());
        post.getLikes().add(mAuth.getUid());
        postAdapter.notifyItemChanged(pos);
        FirestoreClient.getInstance().getPostsCollection().document(String.valueOf(post.getPostedTimeInMillis()))
                .update(Post.LIKES, FieldValue.arrayUnion(mAuth.getUid()))
                .addOnFailureListener(e -> {
                    post.getLikes().remove(mAuth.getUid());
                    postAdapter.notifyItemChanged(pos);
                    Log.i(TAG, "onLiked: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to Like Post", Toast.LENGTH_SHORT).show();
                });
    }

    private void unLikePost(int pos) {
        Post post = posts.get(pos);
        if (post.getLikes() == null)
            post.setLikes(new ArrayList<>());
        post.getLikes().remove(mAuth.getUid());
        postAdapter.notifyItemChanged(pos);
        FirestoreClient.getInstance().getPostsCollection().document(String.valueOf(post.getPostedTimeInMillis()))
                .update(Post.LIKES, FieldValue.arrayRemove(mAuth.getUid()))
                .addOnFailureListener(e -> {
                    post.getLikes().add(mAuth.getUid());
                    postAdapter.notifyItemChanged(pos);
                    Log.i(TAG, "onLiked: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to UnLike Post", Toast.LENGTH_SHORT).show();
                });
    }

    private void showCommentsSections(int pos) {
        Intent intent = new Intent(requireContext(), PostActivity.class);
        intent.putExtra("pos", pos);
        intent.putExtra("postJson", GsonUtil.toJson(posts.get(pos)));
        intent.putExtra("currentUserName", student.getFullName());
        intent.putExtra("hasProfilePic", student.getHasProfilePic());
        startActivityForResult(intent, PostActivity.POST_ACTIVITY_REQUEST);
    }

    // Pagination
    private void paginate() {
        Log.i(TAG, "paginate: ");
        getPosts();
    }

    private void getPosts() {
        if (!endOfCollection) {
            if (student.getFollowing() != null && !student.getFollowing().isEmpty()) {
                Query homeFeedQuery;
                if (lastPost != null)
                    homeFeedQuery = FirestoreClient.getInstance().getPostsCollection().orderBy(Post.POSTED_TIME_IN_MILLIS, Query.Direction.DESCENDING)
                            .whereIn(Post.UID, student.getFollowing())
                            .startAfter(lastPost)
                            .limit(homeFeedLimit);
                else
                    homeFeedQuery = FirestoreClient.getInstance().getPostsCollection().orderBy(Post.POSTED_TIME_IN_MILLIS, Query.Direction.DESCENDING)
                            .whereIn(Post.UID, student.getFollowing())
                            .limit(homeFeedLimit);
                homeFeedQuery.get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            posts.addAll(queryDocumentSnapshots.toObjects(Post.class));
                            if (!refreshing) {
                                binding.homeFeed.setHasFixedSize(false);
                                postAdapter.notifyItemRangeInserted(posts.size() - queryDocumentSnapshots.size(), queryDocumentSnapshots.size());
                                binding.homeFeed.setHasFixedSize(true);
                            } else {
                                refreshing = false;
                                binding.homeRefreshLayout.setRefreshing(false);
                                binding.homeFeedBottomPb.setVisibility(View.VISIBLE);
                                binding.homeFeed.setAdapter(postAdapter);
                                postAdapter.notifyDataSetChanged();
                            }

                            if (firstSetOfPosts) {
                                binding.homeFeedPb.setVisibility(View.GONE);
                                if (posts.size() == 0) {
                                    binding.homeFeedHelperIllustration.setVisibility(View.VISIBLE);
                                    binding.homeFeedHelperText.setVisibility(View.VISIBLE);
                                    if (student.getFollowing() == null || !student.getFollowing().isEmpty())
                                        binding.homeFeedHelperText.setText("Follow more students to see their Posts");
                                    return;
                                }
                                binding.homeFeed.setVisibility(View.VISIBLE);
                                binding.homeFeedBottomPb.setVisibility(View.VISIBLE);
                                firstSetOfPosts = false;
                            }

                            if (queryDocumentSnapshots.size() < homeFeedLimit) {
                                endOfCollection = true;
                                Log.i(TAG, "getPosts: endOfCollection");
                                binding.homeFeedBottomPb.setVisibility(View.GONE);
                            } else
                                lastPost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                            loading = true;

                            Log.i(TAG, "getPosts: " + posts.size());
                        })
                        .addOnFailureListener(e -> {
                            if (mAuth.getCurrentUser() != null)
                                Toast.makeText(requireContext(), "Failed to Load Posts", Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "getPosts: " + e.getMessage());
                        });
            } else {
                endOfCollection = true;
                loading = false;
                binding.homeFeedPb.setVisibility(View.GONE);
                binding.homeFeedHelperIllustration.setVisibility(View.VISIBLE);
                binding.homeFeedHelperText.setVisibility(View.VISIBLE);
                if (refreshing) {
                    refreshing = false;
                    binding.homeRefreshLayout.setRefreshing(false);
                    binding.homeFeed.setAdapter(postAdapter);
                    postAdapter.notifyDataSetChanged();
                }
            }
        } else {
            Log.i(TAG, "getPosts: endOfCollection");
            binding.homeFeedBottomPb.setVisibility(View.GONE);
        }
    }

    private void openNewPostActivity() {
        Intent intent = new Intent(requireContext(), NewPostActivity.class);
        intent.putExtra("currentUserName", student.getFullName());
        intent.putExtra("uid", mAuth.getUid());
        startActivityForResult(intent, NEW_POST_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NEW_POST_REQUEST) {
            if (resultCode == RESULT_OK) {
                assert data != null;
                listener.onNewPost(GsonUtil.fromJson(data.getStringExtra("newPostJson"), Post.class));
            }
        }
        if (requestCode == PostActivity.POST_ACTIVITY_REQUEST && data != null) {
            if (resultCode == PostActivity.POST_CHANGED) {
                int pos = data.getIntExtra("pos", 0);
                Post changedPost = GsonUtil.fromJson(data.getStringExtra("changedPost"), Post.class);
                posts.set(pos, changedPost);
                postAdapter.notifyItemChanged(pos);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.homeFeedScrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollChangedListener);
        binding = null;
    }
}
