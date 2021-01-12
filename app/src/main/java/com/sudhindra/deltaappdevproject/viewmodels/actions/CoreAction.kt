package com.sudhindra.deltaappdevproject.viewmodels.actions

sealed class CoreAction {
    data class SigIn(val email: String, val password: String) : CoreAction()
}
