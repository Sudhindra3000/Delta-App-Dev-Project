package com.sudhindra.deltaappdevproject.firebase.constants

import com.google.firebase.firestore.FirebaseFirestore

object FirestoreConstants {

    // Firestore Database
    private val DB = FirebaseFirestore.getInstance()

    // Collections
    private val CONFIDENTIAL_API_KEYS = DB.collection("confidentialAPIKeys")
    val USERS_COLLECTION = DB.collection("users")

    // Documents
    val ALGOLIA_API_KEYS = CONFIDENTIAL_API_KEYS.document("algoliaAPIKeys")
}
