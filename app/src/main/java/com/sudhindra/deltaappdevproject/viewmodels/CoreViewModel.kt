package com.sudhindra.deltaappdevproject.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sudhindra.deltaappdevproject.models.Student
import com.sudhindra.deltaappdevproject.viewmodels.actions.CoreAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CoreViewModel @ViewModelInject constructor(
        private val auth: FirebaseAuth
) : BaseViewModel<CoreAction>() {

    var student: Student? = null

    override fun doAction(action: CoreAction) = when (action) {
        is CoreAction.SigIn -> signIn(action.email, action.password)
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
}
