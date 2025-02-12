package com.example.docchat.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.docchat.ui.Chat
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {

    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> get() = _chats

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _unreadMessagesCount = MutableLiveData<Int>()
    val unreadMessagesCount: LiveData<Int> get() = _unreadMessagesCount

    fun fetchUnreadMessagesCount(userEmail: String) {
        repository.getUnreadMessagesCount(userEmail) { count ->
            _unreadMessagesCount.postValue(count)
        }
    }

    fun listenForUnreadMessages(userEmail: String) {
        FirebaseFirestore.getInstance().collection("chats")
            .whereArrayContains("participants", userEmail)
            .addSnapshotListener { snapshots, _ ->
                snapshots?.let {
                    fetchUnreadMessagesCount(userEmail)
                    repository.getUnreadMessagesCount(userEmail) { count ->
                        _unreadMessagesCount.postValue(count)
                    }
                }
            }
    }

    fun loadChats(currentEmail: String) {
        repository.fetchChats(currentEmail) { fetchedChats ->
            val updatedChats = fetchedChats.toMutableList()

            fetchedChats.forEachIndexed { index, chat ->
                val otherEmail = chat.participants.firstOrNull { it != currentEmail }
                if (otherEmail != null) {
                    repository.fetchParticipantName(otherEmail) { name ->
                        updatedChats[index] = chat.copy(participantName = name)
                        _chats.postValue(updatedChats)
                    }
                }
            }
        }
    }
}

