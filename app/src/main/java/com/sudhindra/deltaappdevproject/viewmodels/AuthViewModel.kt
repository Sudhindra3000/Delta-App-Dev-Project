package com.sudhindra.deltaappdevproject.viewmodels

import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.iid.FirebaseInstanceId
import com.sudhindra.deltaappdevproject.firebase.constants.FirestoreConstants
import com.sudhindra.deltaappdevproject.firebase.utils.profileImageRef
import com.sudhindra.deltaappdevproject.firebase.utils.userDoc
import com.sudhindra.deltaappdevproject.models.Student
import com.sudhindra.deltaappdevproject.services.MyFirebaseMessagingService
import com.sudhindra.deltaappdevproject.viewmodels.actions.AuthAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel @ViewModelInject constructor(
        private val auth: FirebaseAuth,
        private val messagingService: MyFirebaseMessagingService
) : BaseViewModel<AuthAction>() {

    var student: Student? = null

    private var mutableAPIKeySuccess = MutableLiveData<DocumentSnapshot>()
    val APIKeySuccess: LiveData<DocumentSnapshot> get() = mutableAPIKeySuccess

    private var mutableSignUpSuccess = MutableLiveData<AuthResult>()
    val signUpSuccess: LiveData<AuthResult> get() = mutableSignUpSuccess

    private var mutableProfileUploadSuccess = MutableLiveData<Unit>()
    val profileUploadSuccess: LiveData<Unit> get() = mutableProfileUploadSuccess

    override fun doAction(action: AuthAction) = when (action) {
        is AuthAction.GetAlgoliaAPIKeys -> getAlgoliaAPIKeys()
        is AuthAction.SigUp -> signUp(action.email, action.password, action.student)
        is AuthAction.SigIn -> signIn(action.email, action.password)
        is AuthAction.UploadProfile -> uploadProfile(action.uri)
    }

    private fun getAlgoliaAPIKeys() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val docSnapShot = FirestoreConstants.ALGOLIA_API_KEYS.get().await()
            mutableAPIKeySuccess.postValue(docSnapShot)
        } catch (e: Exception) {
            e.printStackTrace()
            mutableError.postValue("There was an issue. Try again Later")
        }
    }

    private fun signUp(email: String, password: String, student: Student) = viewModelScope.launch(Dispatchers.IO) {
        try {
            // Creates User
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()

            // Stores User Info in Firestore DB
            userDoc(authResult.user!!.uid).set(student).await()

            // Adds FCM Token to User
            val instanceIdResult = FirebaseInstanceId.getInstance().instanceId.await()
            messagingService.addDeviceTokenToUser(instanceIdResult.token)

            mutableSignUpSuccess.postValue(authResult)
        } catch (e: Exception) {
            e.printStackTrace()
            mutableError.postValue("Failed to Sign Up")
        }
    }

    private fun signIn(email: String, password: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            mutableSuccess.postValue("Logged In")
        } catch (e: Exception) {
            e.printStackTrace()
            mutableError.postValue("Failed to Login")
        }
    }

    private fun uploadProfile(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            profileImageRef(auth.uid!!).putFile(uri).await()
            userDoc(auth.uid!!).update(Student.HAS_PROFILE_PIC, true).await()
            mutableProfileUploadSuccess.postValue(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            mutableError.postValue("Failed to Upload Profile")
        }
    }
}
