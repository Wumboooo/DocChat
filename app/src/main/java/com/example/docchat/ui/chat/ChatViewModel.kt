package com.example.docchat.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.docchat.ui.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    fun loadMessages(chatId: String) {
        chatRepository.loadMessages(chatId) { newMessages ->
            _messages.postValue(newMessages)
        }
    }

    fun sendMessage(chatId: String, auth: FirebaseAuth, text: String) {
        chatRepository.sendMessage(chatId, auth, text) {}
    }


    fun forwardMessage(firestore: FirebaseFirestore, chatId: String, callback: (Boolean, List<DocumentSnapshot>?) -> Unit) {
        val doctorsRef = firestore.collection("doctors")
        doctorsRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                callback(false, null)
                return@addOnSuccessListener
            }
            callback(true, snapshot.documents)
        }
    }
}

