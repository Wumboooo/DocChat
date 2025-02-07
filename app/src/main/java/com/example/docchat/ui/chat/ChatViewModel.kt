package com.example.docchat.ui.chat

import android.content.Context
import android.util.Log
import androidx.datastore.core.IOException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.docchat.ui.Message
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class ChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    fun loadMessages(chatId: String) {
        chatRepository.loadMessages(chatId) { newMessages ->
            _messages.postValue(newMessages)
        }
    }

    fun sendMessage(chatId: String, auth: FirebaseAuth, text: String, context: Context) {
        chatRepository.sendMessage(chatId, auth, text, context) {
            sendPushNotification(chatId, text, context) // Pastikan ini tidak mengharapkan parameter
        }
    }


    private fun sendPushNotification(chatId: String, message: String, context: Context) {
        FirebaseFirestore.getInstance().collection("chats")
            .document(chatId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val participants = document.get("participants") as? List<String> ?: emptyList()
                    for (participant in participants) {
                        if (participant != FirebaseAuth.getInstance().currentUser?.email) {
                            FirebaseFirestore.getInstance().collection("users")
                                .document(participant)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val fcmToken = userDoc.getString("fcmToken")
                                    fcmToken?.let {
                                        sendNotificationToUser(it, message, context)
                                    }
                                }
                        }
                    }
                }
            }
    }

    private fun sendNotificationToUser(token: String, message: String, context: Context) {
        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(context)
        googleSignInAccount?.let { account ->
            account.account?.let { googleAccount ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val oauth2Key = GoogleAuthUtil.getToken(
                            context,
                            googleAccount,
                            "oauth2:https://www.googleapis.com/auth/cloud-platform"
                        )

                        sendFCMRequest(oauth2Key, token, message)
                    } catch (e: Exception) {
                        Log.e("FCM", "Failed to get OAuth2 token: ${e.message}")
                    }
                }
            }
        }
    }

    private fun sendFCMRequest(oauth2Key: String, token: String, message: String) {
        val jsonObject = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", token)
                put("notification", JSONObject().apply {
                    put("title", "Pesan Baru")
                    put("body", message)
                })
            })
        }

        val requestBody = jsonObject.toString()
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://fcm.googleapis.com/v1/projects/docchat-187c2/messages:send")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .addHeader("Authorization", "Bearer $oauth2Key")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FCM", "Error sending notification: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("FCM", "Notification sent successfully: ${response.body?.string()}")
            }
        })
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


