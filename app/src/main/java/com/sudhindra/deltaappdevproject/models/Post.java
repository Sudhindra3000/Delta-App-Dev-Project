package com.sudhindra.deltaappdevproject.models;

import android.view.View;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

import com.google.firebase.firestore.Exclude;
import com.sudhindra.deltaappdevproject.GlideApp;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.CloudStorageClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class Post {

    public static final String UID = "uid", POSTED_TIME_IN_MILLIS = "postedTimeInMillis", LIKES = "likes", COMMENTS_N = "commentsN";
    private String uid;
    private String userName;
    /**
     * postedTimeInMillis is also the name of the Post Image File (if exists) in the "posts" folder of Firebase Storage
     * postedTimeInMillis is also the name of Comments Document (if exists) in the "comments" collection of Firebase Cloud Firestore
     */
    private long postedTimeInMillis;
    private String postDescription;
    private boolean imagePost;
    private ArrayList<String> likes;
    private int commentsN;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getPostedTimeInMillis() {
        return postedTimeInMillis;
    }

    public void setPostedTimeInMillis(long postedTimeInMillis) {
        this.postedTimeInMillis = postedTimeInMillis;
    }

    public String getPostDescription() {
        return postDescription;
    }

    public void setPostDescription(String postDescription) {
        this.postDescription = postDescription;
    }

    public boolean isImagePost() {
        return imagePost;
    }

    public void setImagePost(boolean imagePost) {
        this.imagePost = imagePost;
    }

    public ArrayList<String> getLikes() {
        return likes;
    }

    public void setLikes(ArrayList<String> likes) {
        this.likes = likes;
    }

    public int getCommentsN() {
        return commentsN;
    }

    public void setCommentsN(int commentsN) {
        this.commentsN = commentsN;
    }

    @Exclude
    public String getPostDate() {
        SimpleDateFormat fmt = new SimpleDateFormat("MMM dd yyyy");
        Date postDate = new Date(postedTimeInMillis);
        return fmt.format(postDate);
    }

    @BindingAdapter("android:loadPost")
    public static void loadPost(ImageView imageView, Post post) {
        if (post.isImagePost())
            GlideApp.with(imageView)
                    .load(CloudStorageClient.getInstance().getPostsStorage().child(String.valueOf(post.getPostedTimeInMillis())))
                    .placeholder(R.drawable.default_post_image)
                    .into(imageView);
        else
            imageView.setVisibility(View.GONE);
    }

    @BindingAdapter("android:loadPostProfile")
    public static void loadPostProfile(CircleImageView circleImageView, Post post) {
        GlideApp.with(circleImageView)
                .load(CloudStorageClient.getInstance().getProfileImgRef(post.getUid()))
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(circleImageView);
    }
}
