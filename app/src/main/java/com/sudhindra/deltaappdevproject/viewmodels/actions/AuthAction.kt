package com.sudhindra.deltaappdevproject.viewmodels.actions

import android.net.Uri
import com.sudhindra.deltaappdevproject.models.Student

sealed class AuthAction {
    data class GetAlgoliaAPIKeys(val unit: Unit) : AuthAction()
    data class SigUp(val email: String, val password: String, val student: Student) : AuthAction()
    data class SigIn(val email: String, val password: String) : AuthAction()
    data class UploadProfile(val uri: Uri) : AuthAction()
}
