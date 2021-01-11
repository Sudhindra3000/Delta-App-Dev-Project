package com.sudhindra.deltaappdevproject.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.sudhindra.deltaappdevproject.GlideApp;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.activities.ChatActivity;
import com.sudhindra.deltaappdevproject.activities.LoginSignUpActivity;
import com.sudhindra.deltaappdevproject.activities.NewProfilePhotoActivity;
import com.sudhindra.deltaappdevproject.activities.PostActivity;
import com.sudhindra.deltaappdevproject.adapters.PostAdapter;
import com.sudhindra.deltaappdevproject.clients.CloudStorageClient;
import com.sudhindra.deltaappdevproject.clients.ExternalStorageClient;
import com.sudhindra.deltaappdevproject.clients.FilesClient;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.clients.TextRecognitionClient;
import com.sudhindra.deltaappdevproject.databinding.FragmentProfileBinding;
import com.sudhindra.deltaappdevproject.models.Post;
import com.sudhindra.deltaappdevproject.models.Student;
import com.sudhindra.deltaappdevproject.utils.ShareUtil;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    public static final int CURRENT_USER_MODE = 789, OTHER_USER_MODE = 901;
    private int mode, postLimit = 5;
    private FragmentProfileBinding binding;

    private Gson gson = new Gson();

    private TextRecognitionClient textRecognitionClient;

    // User Data
    private FirebaseAuth mAuth;
    private Student student;

    private SharedPreferences sharedPreferences;

    // Post Data
    private DocumentSnapshot lastPost = null;
    private boolean firstSetOfPosts = true, endOfCollection = false, loading = true, refreshing = false;

    // UI
    private ArrayList<Post> posts;
    private PostAdapter postAdapter;

    private ViewTreeObserver.OnScrollChangedListener scrollChangedListener = () -> {
        View view = binding.profileFragScrollView.getChildAt(binding.profileFragScrollView.getChildCount() - 1);

        int diff = (view.getBottom() - (binding.profileFragScrollView.getHeight() + binding.profileFragScrollView.getScrollY()));

        if (diff == 0 && loading) {
            loading = false;
            paginate();
        }
    };

    private PostAdapter.PostAdapterListener postAdapterListener = new PostAdapter.PostAdapterListener() {
        @Override
        public void onProfileClicked(int pos) {
        }

        @Override
        public void onTextRecognitionClicked(ImageView imageView) {
            processImage(imageView);
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
            unlikePost(pos);
        }

        @Override
        public void onCommentClicked(int pos) {
            showCommentsSection(pos);
        }
    };

    //    OTHER_USER_MODE
    private String otherUserId, currentUserName;
    private int followersNum;

    public ProfileFragment() {

    }

    public interface ProfileFragmentListener {
        void onLogout();
    }

    public ProfileFragment(int mode) {
        this.mode = mode;
    }

    public void setStudent(Student student) {
        this.student = student;
        if (student.getFollowers() != null)
            followersNum = student.getFollowers().size();
        else
            followersNum = 0;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName;
    }

    public void onReselected() {
        binding.profileFragScrollView.smoothScrollTo(0, 0);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(firebaseAuth -> mAuth = firebaseAuth);
        sharedPreferences = requireContext().getSharedPreferences("userPref", Context.MODE_PRIVATE);
        if (mode == CURRENT_USER_MODE) {
            FirestoreClient.getInstance().getUserInfoDocRef(mAuth.getUid()).addSnapshotListener((documentSnapshot, e) -> {
                if (e != null)
                    Log.i(TAG, "onCreate: " + e.getMessage());
                if (documentSnapshot != null) {
                    student = documentSnapshot.toObject(Student.class);
                    currentUserName = student.getFullName();
                    sharedPreferences.edit().putString("currentUserName", currentUserName).apply();
                    if (mAuth.getCurrentUser() != null)
                        updateUI();
                }
            });
        }

        textRecognitionClient = new TextRecognitionClient(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        buildRecyclerView();

        if (mode == CURRENT_USER_MODE) {
            getCurrentUserPosts();
            loading = false;
            binding.changeProfileBt.setOnClickListener(v -> showNewProfileScreen());
            binding.logOutBt.setOnClickListener(v -> logout());
        } else {
            getNewUserPosts();
            loading = false;
            binding.profileToolbar.setVisibility(View.GONE);
            binding.changeProfileBt.setVisibility(View.GONE);
            binding.logOutBt.setVisibility(View.GONE);
            binding.messageBt.setVisibility(View.VISIBLE);
            binding.followBt.setOnClickListener(v -> follow());
            binding.followingBt.setOnClickListener(v -> unFollow());
            binding.messageBt.setOnClickListener(v -> messageUser());
        }

        binding.profileToolbar.setOnClickListener(v -> onReselected());
        binding.profileFragRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        binding.profileFragRefreshLayout.setOnRefreshListener(this::refresh);
    }

    private void buildRecyclerView() {
        binding.userPosts.setHasFixedSize(true);
        // To remove the Default Dim animation when onItemChanged() is called
        ((SimpleItemAnimator) Objects.requireNonNull(binding.userPosts.getItemAnimator())).setSupportsChangeAnimations(false);
        posts = new ArrayList<>();
        postAdapter = new PostAdapter(requireContext(), mAuth.getUid());
        postAdapter.setPosts(posts);
        binding.userPosts.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.userPosts.setAdapter(postAdapter);

        postAdapter.setListener(postAdapterListener);

        binding.profileFragScrollView.getViewTreeObserver().addOnScrollChangedListener(scrollChangedListener);
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
        if (mode == CURRENT_USER_MODE)
            FirestoreClient.getInstance().getUsersCollection().document(mAuth.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        student = documentSnapshot.toObject(Student.class);
                        updateUI();
                        getCurrentUserPosts();
                    })
                    .addOnFailureListener(e -> {
                        Log.i(TAG, "refresh: " + e.getMessage());
                        Toast.makeText(requireActivity(), "Failed to Refresh", Toast.LENGTH_SHORT).show();
                    });
        else
            FirestoreClient.getInstance().getUsersCollection().document(otherUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        student = documentSnapshot.toObject(Student.class);
                        updateUI();
                        getNewUserPosts();
                    })
                    .addOnFailureListener(e -> {
                        Log.i(TAG, "refresh: " + e.getMessage());
                        Toast.makeText(requireActivity(), "Failed to Refresh", Toast.LENGTH_SHORT).show();
                    });
    }

    public void updateUI() {
        if (binding != null) {
            getProfileImg();
            binding.userFullName.setText(student.getFullName());
            binding.userInfo.setText(student.getYOSString() + " year " + getResources().getStringArray(R.array.branch_items)[student.getBranch()] + " Student at NIT-T");
            if (mode == OTHER_USER_MODE)
                if (student.getFollowers() == null || !student.getFollowers().contains(FirebaseAuth.getInstance().getUid()))
                    binding.followBt.setVisibility(View.VISIBLE);
                else
                    binding.followingBt.setVisibility(View.VISIBLE);
            if (student.getFollowers() != null)
                binding.followersN.setText(String.valueOf(student.getFollowers().size()));
            if (student.getFollowing() != null)
                binding.followingN.setText(String.valueOf(student.getFollowing().size()));
        }
    }

    private void showNewProfileScreen() {
        Intent intent = new Intent(requireContext(), NewProfilePhotoActivity.class);
        intent.putExtra("student", gson.toJson(student));
        startActivity(intent);
    }

    public void updateNewPost(Post post) {
        binding.userPosts.setHasFixedSize(false);
        if (!firstSetOfPosts) {
            posts.add(0, post);
            postAdapter.notifyItemInserted(0);
        } else {
            posts.add(post);
            postAdapter.notifyDataSetChanged();
            binding.addPostsIllustration.setVisibility(View.GONE);
            binding.addPostsMessage.setVisibility(View.GONE);
            binding.userPosts.setVisibility(View.VISIBLE);
            firstSetOfPosts = false;
        }
        binding.userPosts.setHasFixedSize(true);
        Log.i(TAG, "updateNewPost: ");
    }

    private void getProfileImg() {
        if (mode == CURRENT_USER_MODE) {
            GlideApp.with(this)
                    .load(CloudStorageClient.getInstance().getProfileImgRef(mAuth.getUid()))
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(binding.profileImageProfile);
        } else {
            GlideApp.with(this)
                    .load(CloudStorageClient.getInstance().getProfileImgRef(otherUserId))
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(binding.profileImageProfile);
        }
    }

    // Post Actions
    private void processImage(ImageView imageView) {
        textRecognitionClient.processImage(imageView);
    }

    private void sharePost(int pos, ImageView imageView) {
        Post post = posts.get(pos);
        String head, body = post.getPostDescription();
        if (mode == CURRENT_USER_MODE)
            head = "Checkout this Post by me in " + getResources().getString(R.string.app_name) + " on " + post.getPostDate() + ":";
        else
            head = "Checkout this Post by " + post.getUserName() + " in " + getResources().getString(R.string.app_name) + " on " + post.getPostDate() + ":";
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

    private void unlikePost(int pos) {
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

    private void showCommentsSection(int pos) {
        Intent intent = new Intent(requireContext(), PostActivity.class);
        intent.putExtra("pos", pos);
        intent.putExtra("postJson", gson.toJson(posts.get(pos)));
        intent.putExtra("currentUserName", currentUserName);
        startActivityForResult(intent, PostActivity.POST_ACTIVITY_REQUEST);
    }

    // Pagination
    private void paginate() {
        Log.i(TAG, "paginate: ");
        if (mode == CURRENT_USER_MODE)
            getCurrentUserPosts();
        else
            getNewUserPosts();
    }

    private void getCurrentUserPosts() {
        if (!endOfCollection) {
            Query postsQuery;
            if (lastPost != null)
                postsQuery = FirestoreClient.getInstance().getPostsCollection().orderBy(Post.POSTED_TIME_IN_MILLIS, Query.Direction.DESCENDING)
                        .whereEqualTo(Post.UID, mAuth.getUid())
                        .startAfter(lastPost)
                        .limit(postLimit);
            else
                postsQuery = FirestoreClient.getInstance().getPostsCollection().orderBy(Post.POSTED_TIME_IN_MILLIS, Query.Direction.DESCENDING)
                        .whereEqualTo(Post.UID, mAuth.getUid())
                        .limit(postLimit);
            postsQuery.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        posts.addAll(queryDocumentSnapshots.toObjects(Post.class));
                        if (!refreshing) {
                            binding.userPosts.setHasFixedSize(false);
                            postAdapter.notifyItemRangeInserted(posts.size() - queryDocumentSnapshots.size(), queryDocumentSnapshots.size());
                            binding.userPosts.setHasFixedSize(true);
                        } else {
                            refreshing = false;
                            binding.profileFragRefreshLayout.setRefreshing(false);
                            if (!posts.isEmpty())
                                binding.profileFragBottomPb.setVisibility(View.VISIBLE);
                            binding.userPosts.setAdapter(postAdapter);
                            postAdapter.notifyDataSetChanged();
                        }

                        if (firstSetOfPosts) {
                            binding.postsProgressBar.setVisibility(View.GONE);
                            if (posts.size() == 0) {
                                binding.addPostsIllustration.setVisibility(View.VISIBLE);
                                binding.addPostsMessage.setVisibility(View.VISIBLE);
                                loading = false;
                                return;
                            }
                            binding.userPosts.setVisibility(View.VISIBLE);
                            binding.profileFragBottomPb.setVisibility(View.VISIBLE);
                            firstSetOfPosts = false;
                        }

                        if (queryDocumentSnapshots.size() < postLimit) {
                            endOfCollection = true;
                            Log.i(TAG, "getCurrentUserPosts: endOfCollection");
                            binding.profileFragBottomPb.setVisibility(View.GONE);
                        } else
                            lastPost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                        loading = true;

                        Log.i(TAG, "getUserPosts: " + posts.size());

                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireActivity(), "Failed to Load Posts", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "getUserPosts: " + e.getMessage());
                    });
        } else {
            Log.i(TAG, "getCurrentUserPosts: endOfCollection");
            binding.profileFragBottomPb.setVisibility(View.GONE);
        }
    }

    private void logout() {
        // Todo: Delete all Message Files on logout
        AtomicBoolean deleteMessageFiles = new AtomicBoolean(false);
        new MaterialAlertDialogBuilder(requireContext(), R.style.AppTheme_LogoutDialogTheme)
                .setTitle("Log out")
                .setMultiChoiceItems(R.array.logoutOptions, new boolean[]{false}, (dialogInterface, i, b) -> {
                    switch (i) {
                        case 0:
                            deleteMessageFiles.set(b);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {

                })
                .setPositiveButton("Ok", ((dialog, which) -> {
                    if (deleteMessageFiles.get()) {
                        try {
                            FilesClient.deleteAllMessageFiles(requireContext());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    ProgressDialog progressDialog = new ProgressDialog(requireContext(), R.style.AppTheme_LogoutProgressDialogTheme);
                    progressDialog.setMessage("Logging Out");
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();
                    FirebaseInstanceId.getInstance().getInstanceId()
                            .addOnSuccessListener(instanceIdResult -> {
                                DocumentReference userDocRef = FirestoreClient.getInstance().getUserInfoDocRef(mAuth.getUid());
                                userDocRef.update(Student.REGISTRATION_TOKENS, FieldValue.arrayRemove(instanceIdResult.getToken()))
                                        .addOnSuccessListener(aVoid -> {
                                            sharedPreferences.edit().remove("currentUserName").apply();
                                            mAuth.signOut();
                                            progressDialog.dismiss();
                                            startActivity(new Intent(requireContext(), LoginSignUpActivity.class));
                                            requireActivity().finish();
                                        })
                                        .addOnFailureListener(e -> Log.i(TAG, "logout: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> Log.i(TAG, "logout: " + e.getMessage()));
                }))
                .show();
    }

    //    OTHER_USER_MODE
    private void follow() {
        binding.followBt.setVisibility(View.GONE);
        binding.followingBt.setVisibility(View.VISIBLE);
        followersNum++;
        binding.followersN.setText(String.valueOf(followersNum));
        WriteBatch followBatch = FirebaseFirestore.getInstance().batch();
        followBatch.update(FirestoreClient.getInstance().getUsersCollection().document(otherUserId), Student.FOLLOWERS_KEY, FieldValue.arrayUnion(FirebaseAuth.getInstance().getUid()));
        followBatch.update(FirestoreClient.getInstance().getUsersCollection().document(FirebaseAuth.getInstance().getUid()), Student.FOLLOWING_KEY, FieldValue.arrayUnion(otherUserId));
        followBatch.commit()
                .addOnFailureListener(e -> {
                    Toast.makeText(requireActivity(), "Failed to Follow " + student.getFullName(), Toast.LENGTH_SHORT).show();
                    binding.followBt.setVisibility(View.VISIBLE);
                    binding.followingBt.setVisibility(View.GONE);
                    followersNum--;
                    binding.followersN.setText(String.valueOf(followersNum));
                    Log.i(TAG, "follow: " + e.getMessage());
                });
    }

    private void unFollow() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Unfollow")
                .setMessage("Stop following " + student.getFullName() + "?")
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    binding.followingBt.setVisibility(View.GONE);
                    binding.followBt.setVisibility(View.VISIBLE);
                    followersNum--;
                    binding.followersN.setText(String.valueOf(followersNum));
                    WriteBatch unFollowBatch = FirebaseFirestore.getInstance().batch();
                    unFollowBatch.update(FirestoreClient.getInstance().getUsersCollection().document(otherUserId), Student.FOLLOWERS_KEY, FieldValue.arrayRemove(FirebaseAuth.getInstance().getUid()));
                    unFollowBatch.update(FirestoreClient.getInstance().getUsersCollection().document(FirebaseAuth.getInstance().getUid()), Student.FOLLOWING_KEY, FieldValue.arrayRemove(otherUserId));
                    unFollowBatch.commit()
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireActivity(), "Failed to Unfollow " + student.getFullName(), Toast.LENGTH_SHORT).show();
                                binding.followBt.setVisibility(View.GONE);
                                binding.followingBt.setVisibility(View.VISIBLE);
                                followersNum++;
                                binding.followersN.setText(String.valueOf(followersNum));
                                Log.i(TAG, "unFollow: " + e.getMessage());
                            });
                })
                .setNegativeButton("No", (dialogInterface, i) -> {
                })
                .show();
    }

    private void messageUser() {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("currentUserName", currentUserName);
        intent.putExtra("otherUserId", otherUserId);
        intent.putExtra("otherUserName", student.getFullName());
        startActivity(intent);
    }

    private void getNewUserPosts() {
        if (!endOfCollection) {
            Query query;
            if (lastPost != null)
                query = FirestoreClient.getInstance().getPostsCollection().orderBy(Post.POSTED_TIME_IN_MILLIS, Query.Direction.DESCENDING)
                        .whereEqualTo(Post.UID, otherUserId)
                        .startAfter(lastPost)
                        .limit(postLimit);
            else
                query = FirestoreClient.getInstance().getPostsCollection().orderBy(Post.POSTED_TIME_IN_MILLIS, Query.Direction.DESCENDING)
                        .whereEqualTo(Post.UID, otherUserId)
                        .limit(postLimit);
            query.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        posts.addAll(queryDocumentSnapshots.toObjects(Post.class));
                        if (!refreshing) {
                            binding.userPosts.setHasFixedSize(false);
                            postAdapter.notifyItemRangeInserted(posts.size() - queryDocumentSnapshots.size(), queryDocumentSnapshots.size());
                            binding.userPosts.setHasFixedSize(true);
                        } else {
                            refreshing = false;
                            binding.profileFragRefreshLayout.setRefreshing(false);
                            if (!posts.isEmpty())
                                binding.profileFragBottomPb.setVisibility(View.VISIBLE);
                            binding.userPosts.setAdapter(postAdapter);
                        }

                        if (firstSetOfPosts) {
                            binding.postsProgressBar.setVisibility(View.GONE);
                            if (posts.size() == 0) {
                                binding.noPostsTv.setVisibility(View.VISIBLE);
                                return;
                            }
                            binding.userPosts.setVisibility(View.VISIBLE);
                            binding.profileFragBottomPb.setVisibility(View.VISIBLE);
                            firstSetOfPosts = false;
                        }

                        if (queryDocumentSnapshots.size() < postLimit) {
                            endOfCollection = true;
                            Log.i(TAG, "getNewUserPosts: endOfCollection");
                            binding.profileFragBottomPb.setVisibility(View.GONE);
                        } else
                            lastPost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                        loading = true;

                        Log.i(TAG, "getNewUserPosts: " + posts.size());
                        Log.i(TAG, "getNewUserPosts: success");

                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireActivity(), "Failed to Load Posts", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "getNewUserPosts: " + e.getMessage());
                    });
        } else {
            Log.i(TAG, "getNewUserPosts: endOfCollection");
            binding.profileFragBottomPb.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PostActivity.POST_ACTIVITY_REQUEST && data != null) {
            if (resultCode == PostActivity.POST_CHANGED) {
                int pos = data.getIntExtra("pos", 0);
                Post changedPost = gson.fromJson(data.getStringExtra("changedPost"), Post.class);
                posts.set(pos, changedPost);
                postAdapter.notifyItemChanged(pos);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.profileFragScrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollChangedListener);
        binding = null;
    }
}