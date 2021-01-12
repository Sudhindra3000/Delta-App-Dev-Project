package com.sudhindra.deltaappdevproject.firebase.utils

import com.sudhindra.deltaappdevproject.firebase.constants.CloudStorageConstants

/**
 * Gets a StorageReference to Profile Image of User with UserID = id
 */
fun profileImageRef(id: String) = CloudStorageConstants.PROFILES.child(id)
