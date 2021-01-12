package com.sudhindra.deltaappdevproject.firebase.constants

import com.google.firebase.storage.FirebaseStorage

object CloudStorageConstants {

    // Firebase Cloud Storage
    private val STORAGE = FirebaseStorage.getInstance()

    // Folders
    private val ROOT = STORAGE.reference

    val PROFILES = ROOT.child("profiles")
}
