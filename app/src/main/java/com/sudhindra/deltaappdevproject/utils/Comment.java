package com.sudhindra.deltaappdevproject.utils;

import androidx.databinding.BindingAdapter;

import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.CloudStorageClient;

import de.hdodenhof.circleimageview.CircleImageView;

public class Comment {

    public static final String COMMENT_TIME_IN_MILLIS = "commentTimeInMillis";
    private String uid;
    private String userName;
    private String commentString;
    private long commentTimeInMillis;

    public Comment() {
    }

    public Comment(String uid, String userName, String commentString, long commentTimeInMillis) {
        this.uid = uid;
        this.userName = userName;
        this.commentString = commentString;
        this.commentTimeInMillis = commentTimeInMillis;
    }

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

    public String getCommentString() {
        return commentString;
    }

    public void setCommentString(String commentString) {
        this.commentString = commentString;
    }

    public long getCommentTimeInMillis() {
        return commentTimeInMillis;
    }

    public void setCommentTimeInMillis(long commentTimeInMillis) {
        this.commentTimeInMillis = commentTimeInMillis;
    }

    @BindingAdapter("android:loadCommentProfile")
    public static void loadCommentProfile(CircleImageView circleImageView, String uid) {
        GlideApp.with(circleImageView)
                .load(CloudStorageClient.getInstance().getProfileImgRef(uid))
                .placeholder(R.drawable.default_profile)
                .into(circleImageView);
    }
}
