package com.sudhindra.deltaappdevproject.firebase.utils

import com.sudhindra.deltaappdevproject.firebase.constants.FirestoreConstants

/**
 * Gets a DocumentReference for a User with UserID = id
 */
fun userDoc(id: String) = FirestoreConstants.USERS_COLLECTION.document(id)
