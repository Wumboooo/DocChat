package com.example.docchat.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.docchat.ui.Chat

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {

    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> get() = _chats

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

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

