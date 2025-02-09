package com.example.docchat.ui.chat

import android.content.Context
import android.util.Log
import androidx.datastore.core.IOException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docchat.ui.Messages
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class ChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {
    private val _messages = MutableLiveData<List<Messages>>()
    val messages: LiveData<List<Messages>> get() = _messages

    fun loadMessages(chatId: String) {
        chatRepository.loadMessages(chatId) { newMessages ->
            _messages.postValue(newMessages)
        }
    }

    fun sendMessage(chatId: String, auth: FirebaseAuth, text: String, context: Context) {
        chatRepository.sendMessage(chatId, auth, text, context) {
            sendPushNotification(chatId, text, context)
        }
    }


    private fun sendPushNotification(chatId: String, message: String, context: Context) {
        FirebaseFirestore.getInstance().collection("chats")
            .document(chatId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val participants = document.get("participants") as? List<String> ?: emptyList()
                    Log.d("FCM", "Participants: $participants")

                    for (participant in participants) {
                        if (participant != FirebaseAuth.getInstance().currentUser?.email) {
                            FirebaseFirestore.getInstance().collection("users")
                                .document(participant)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val fcmToken = userDoc.getString("fcmToken")
                                    Log.d("FCM", "FCM Token for $participant: $fcmToken")
                                    fcmToken?.let {
                                        sendFCMRequest(it, message, context)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FCM", "Failed to fetch user data: ${e.message}")
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Failed to fetch chat document: ${e.message}")
            }
    }

    private fun sendFCMRequest(fcmToken: String, message: String, context: Context) {
        viewModelScope.launch {
            val authToken = getAccessToken(context)
            if (authToken == null) {
                Log.e("FCM", "Failed to obtain OAuth token")
                return@launch
            }

            val jsonObject = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", fcmToken)  // Use the recipient's FCM token, not OAuth token!
                    put("notification", JSONObject().apply {
                        put("title", "Pesan Baru")
                        put("body", message)
                    })
                })
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://fcm.googleapis.com/v1/projects/docchat-187c2/messages:send")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $authToken")  // Corrected token usage
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("FCM", "Error sending notification: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("FCM", "Notification sent successfully: ${response.code}, ${response.body?.string()}")
                }
            })
        }
    }

    private suspend fun getAccessToken(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val credentials = GoogleCredentials
                    .fromStream(context.assets.open("service-account-key.json"))
                    .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging")) // Correct scope for FCM
                credentials.refreshIfExpired()
                credentials.accessToken.tokenValue
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
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


