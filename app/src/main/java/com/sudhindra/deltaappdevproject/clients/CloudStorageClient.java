package com.sudhindra.deltaappdevproject.clients;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class CloudStorageClient {

    private static final String PROFILES_STORAGE_PATH = "profiles", POSTS_STORAGE_PATH = "posts", MESSAGES_STORAGE_PATH = "messages";
    private static CloudStorageClient instance;
    private StorageReference rootStorageRef;

    private CloudStorageClient() {
        rootStorageRef = FirebaseStorage.getInstance().getReference();
    }

    public static synchronized CloudStorageClient getInstance() {
        if (instance == null) instance = new CloudStorageClient();
        return instance;
    }

    public StorageReference getProfilesStorage() {
        return rootStorageRef.child(PROFILES_STORAGE_PATH);
    }

    public StorageReference getPostsStorage() {
        return rootStorageRef.child(POSTS_STORAGE_PATH);
    }

    public StorageReference getMessagesStorage() {
        return rootStorageRef.child(MESSAGES_STORAGE_PATH);
    }

    public StorageReference getProfileImgRef(String uid) {
        return getProfilesStorage().child(uid);
    }

    public StorageReference getMessageFileRef(long sentTimeInMillis) {
        return getMessagesStorage().child(String.valueOf(sentTimeInMillis));
    }
}
