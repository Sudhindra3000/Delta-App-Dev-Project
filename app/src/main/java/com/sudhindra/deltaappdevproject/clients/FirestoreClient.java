package com.sudhindra.deltaappdevproject.clients;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sudhindra.deltaappdevproject.services.MyFirebaseMessagingService;

public class FirestoreClient {

    private static final String USERS = "users", POSTS = "posts", CHAT_CHANNELS = "chatChannels", COMMENTS = "comments", CONFIDENTIAL_API_KEYS = "confidentialAPIKeys";
    private static final String ALGOLIA_API_KEYS = "algoliaAPIKeys";
    public static final String ALGOLIA_ADMIN_API_KEY = "adminAPIKey";
    private static FirestoreClient instance;
    private FirebaseFirestore db;
    private MyFirebaseMessagingService messagingService;

    private FirestoreClient() {
        db = FirebaseFirestore.getInstance();
        messagingService = new MyFirebaseMessagingService();
    }

    public static synchronized FirestoreClient getInstance() {
        if (instance == null) instance = new FirestoreClient();
        return instance;
    }

    public CollectionReference getUsersCollection() {
        return db.collection(USERS);
    }

    public CollectionReference getPostsCollection() {
        return db.collection(POSTS);
    }

    public CollectionReference getChatChannelCollection() {
        return db.collection(CHAT_CHANNELS);
    }

    public CollectionReference getCommentsCollections() {
        return db.collection(COMMENTS);
    }

    public CollectionReference getCommentsForPost(long postTime) {
        return getCommentsCollections().document(String.valueOf(postTime)).collection(COMMENTS);
    }

    public DocumentReference getUserInfoDocRef(String uid) {
        return getUsersCollection().document(uid);
    }

    public DocumentReference getAlgoliaAPIKeys() {
        return db.collection(CONFIDENTIAL_API_KEYS).document(ALGOLIA_API_KEYS);
    }

    public DocumentReference getCommentDocRef(long postTime, long commentTime) {
        return getCommentsCollections().document(String.valueOf(postTime)).collection(COMMENTS).document(String.valueOf(commentTime));
    }

    public void addDeviceTokenToUser(String token) {
        messagingService.addDeviceTokenToUser(token);
    }
}
