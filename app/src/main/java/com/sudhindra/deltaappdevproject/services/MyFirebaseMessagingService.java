package com.sudhindra.deltaappdevproject.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.models.Student;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM";
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.i(TAG, "onNewToken: " + s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getNotification() != null) {
            Log.i(TAG, "onMessageReceived: " + remoteMessage.getData().toString());
        }
    }

    public void addDeviceTokenToUser(String token) {
        DocumentReference userDocRef = FirestoreClient.getInstance().getUserInfoDocRef(mAuth.getUid());
        userDocRef.update(Student.REGISTRATION_TOKENS, FieldValue.arrayUnion(token))
                .addOnSuccessListener(aVoid -> Log.i(TAG, "addTokenToFirestore: successfully added token"))
                .addOnFailureListener(e -> Log.i(TAG, "addTokenToFirestore: " + e.getMessage()));
    }
}
