package com.sudhindra.deltaappdevproject.utils;

import androidx.databinding.BindingAdapter;

import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.CloudStorageClient;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class User {

    private String uid, name;

    public User(String uid, String name) {
        this.uid = uid;
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User that = (User) o;
        return getUid().equals(that.getUid()) &&
                getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUid(), getName());
    }

    @BindingAdapter("android:loadUserProfile")
    public static void loadUserProfile(CircleImageView circleImageView, User user) {
        GlideApp.with(circleImageView)
                .load(CloudStorageClient.getInstance().getProfilesStorage().child(user.getUid()))
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(circleImageView);
    }
}
