package com.example.posturometricapp.ui.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.posturometricapp.domain.api.SessionInteractor
import com.example.posturometricapp.domain.model.Session
import kotlinx.coroutines.launch

class SessionListViewModel(
    private val interactor: SessionInteractor
) : ViewModel() {
    val sessions: LiveData<List<Session>> =
        interactor.getAllSessions()
            .asLiveData()

    // Navigation event: emit selected sessionId
    private val _openSession = MutableLiveData<Long>()
    val openSession: LiveData<Long> get() = _openSession

    fun deleteSession(session: Session){
        viewModelScope.launch {
            interactor.deleteSession(session)
        }
    }
    fun onSessionClicked(sessionId: Long) {
        _openSession.value = sessionId
    }
}