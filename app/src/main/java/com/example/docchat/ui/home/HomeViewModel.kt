package com.example.docchat.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docchat.ui.Chat
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {

    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> get() = _chats

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _unreadMessagesCount = MutableLiveData<Map<String, Int>>()
    val unreadMessagesCount: LiveData<Map<String, Int>> get() = _unreadMessagesCount

    fun listenForUnreadMessages(userEmail: String) {
        repository.listenForUnreadMessages(userEmail) { unreadCounts ->
            _unreadMessagesCount.postValue(unreadCounts)
        }
    }

    fun loadChats(currentEmail: String) {
        repository.fetchChats(currentEmail) { fetchedChats ->
            viewModelScope.launch {
                val updatedChats = fetchedChats.map { chat ->
                    val otherEmail = chat.participants.firstOrNull { it != currentEmail }
                    if (otherEmail != null) {
                        val name = fetchParticipantNameSuspend(otherEmail)
                        chat.copy(participantName = name)
                    } else {
                        chat
                    }
                }
                _chats.postValue(updatedChats)
            }
        }
    }

    private suspend fun fetchParticipantNameSuspend(email: String): String =
        suspendCoroutine { cont ->
            repository.fetchParticipantName(email) { name ->
                cont.resume(name)
            }
        }

}

