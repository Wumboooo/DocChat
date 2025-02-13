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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
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

    private var unreadCount = 0

    private val sentNotifications = mutableMapOf<String, Long>()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var chatRef: DocumentReference? = null

    fun loadMessages(chatId: String) {
        chatRepository.loadMessages(chatId) { newMessages ->
            _messages.postValue(newMessages)
            sentNotifications.remove(chatId)
        }
    }

    fun sendMessage(chatId: String, text: String, context: Context) {
        val senderEmail = auth.currentUser?.email ?: return

        chatRepository.sendMessage(chatId, senderEmail, text, context) {
            checkAndSendNotification(chatId, text, context)
        }
    }

    fun markMessagesAsRead(chatId: String) {
        val currentUserEmail = auth.currentUser?.email ?: return
        chatRepository.markMessagesAsRead(chatId, currentUserEmail)
        sentNotifications.remove(chatId)
    }

    fun setUserActiveInChat(chatId: String, isActive: Boolean) {
        val currentUserEmail = auth.currentUser?.email ?: return
        chatRef = firestore.collection("chats").document(chatId)

        firestore.runTransaction { transaction ->
            val chatSnapshot = transaction.get(chatRef!!)
            val activeParticipants = chatSnapshot.get("activeParticipants") as? MutableMap<String, Boolean> ?: mutableMapOf()

            if (isActive) {
                activeParticipants[currentUserEmail] = true
            } else {
                activeParticipants.remove(currentUserEmail)
            }

            transaction.update(chatRef!!, "activeParticipants", activeParticipants)
        }
    }

    fun monitorActiveParticipants(chatId: String, onUpdate: (Map<String, Boolean>) -> Unit) {
        chatRef = firestore.collection("chats").document(chatId)

        chatRef!!.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val activeParticipants = snapshot.get("activeParticipants") as? Map<String, Boolean> ?: emptyMap()
                onUpdate(activeParticipants)
            }
        }
    }

    private fun checkAndSendNotification(chatId: String, message: String, context: Context) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email?.lowercase() ?: return

        FirebaseFirestore.getInstance().collection("chats")
            .document(chatId)
            .get()
            .addOnSuccessListener { chatDoc ->
                val activeParticipants = chatDoc.get("activeParticipants") as? Map<String, Boolean> ?: emptyMap()
                val participants = chatDoc.get("participants") as? List<String> ?: emptyList() // Ambil semua peserta chat

                getUserName(currentUserEmail) { senderName ->
                    for (participant in participants) { // Loop semua peserta chat
                        if (participant.lowercase() != currentUserEmail.lowercase() &&
                            (activeParticipants[participant] == null || activeParticipants[participant] == false) // Kirim hanya jika tidak aktif
                        ) {
                            getUserFCMToken(participant) { fcmToken ->
                                if (fcmToken != null) {
                                    val currentTime = System.currentTimeMillis()

                                    if (!sentNotifications.containsKey(chatId) || currentTime - sentNotifications[chatId]!! > 3000) {
                                        sendFCMRequest(fcmToken, senderName, message, chatId, currentTime, context, participant)
                                        sentNotifications[chatId] = currentTime
                                        unreadCount++
                                    }
                                } else {
                                    Log.w("FCM", "Token tidak ditemukan untuk $participant")
                                    deleteFCMTokenAndRegenerate(participant, context, senderName, message, chatId)
                                }
                            }
                        }
                    }
                }
            }

    }


    private fun getUserFCMToken(email: String, callback: (String?) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val collections = listOf("users", "admins", "doctors")

        fun searchInCollections(index: Int) {
            if (index >= collections.size) {
                callback(null)
                return
            }

            firestore.collection(collections[index])
                .document(email)
                .get()
                .addOnSuccessListener { userDoc ->
                    val fcmToken = userDoc.getString("fcmToken")
                    if (!fcmToken.isNullOrEmpty()) {
                        callback(fcmToken)
                    } else {
                        searchInCollections(index + 1)
                    }
                }
                .addOnFailureListener {
                    searchInCollections(index + 1)
                }
        }

        searchInCollections(0)
    }

    private fun getUserName(email: String, callback: (String) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val collections = listOf("users", "admins", "doctors")

        fun searchName(index: Int) {
            if (index >= collections.size) {
                callback("Pengguna")
                return
            }

            firestore.collection(collections[index])
                .document(email)
                .get()
                .addOnSuccessListener { userDoc ->
                    val name = userDoc.getString("name")
                    if (!name.isNullOrEmpty()) {
                        callback(name)
                    } else {
                        searchName(index + 1)
                    }
                }
                .addOnFailureListener {
                    searchName(index + 1)
                }
        }

        searchName(0)
    }

    private fun sendFCMRequest(
        fcmToken: String, senderName: String, message: String, chatId: String, timestamp: Long, context: Context, recipientEmail: String
    ) {
        viewModelScope.launch {
            val authToken = getAccessToken(context)
            if (authToken == null) {
                Log.e("FCM", "Failed to obtain OAuth token")
                return@launch
            }

            val jsonObject = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", fcmToken)
                    put("notification", JSONObject().apply {
                        put("title", senderName)
                        put("body", message)
                    })
                    put("data", JSONObject().apply {
                        put("title", senderName)
                        put("body", message)
                        put("chatId", chatId)
                    })
                })
            }

            val url = "https://fcm.googleapis.com/v1/projects/docchat-187c2/messages:send"
            val requestBody = jsonObject.toString()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("FCM", "Failed to send notification", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val responseBody = it.body?.string()
                        if (!it.isSuccessful) {
                            Log.e("FCM", "Error sending FCM: $responseBody")

                            if (it.code == 404 && responseBody?.contains("UNREGISTERED") == true) {
                                Log.w("FCM", "FCM token tidak ditemukan. Menghapus token lama dan memperbarui.")
                                deleteFCMTokenAndRegenerate(recipientEmail, context, senderName, message, chatId)
                            } else {
                                Log.d("FCM", "Notification sent successfully for $fcmToken: $responseBody")
                            }
                        } else {
                            Log.d("FCM", "Notification sent successfully for $fcmToken: $responseBody")
                        }
                    }
                }
            })
        }
    }

    private fun deleteFCMTokenAndRegenerate(email: String, context: Context, senderName: String, message: String, chatId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val collections = listOf("users", "admins", "doctors")

        fun removeToken(index: Int) {
            if (index >= collections.size) {
                generateNewFCMToken(email, context, senderName, message, chatId)
                return
            }

            firestore.collection(collections[index])
                .document(email)
                .update("fcmToken", FieldValue.delete())
                .addOnSuccessListener {
                    Log.d("FCM", "Token FCM dihapus untuk $email di koleksi ${collections[index]}")
                    generateNewFCMToken(email, context, senderName, message, chatId)
                }
                .addOnFailureListener {
                    removeToken(index + 1)
                }
        }

        removeToken(0)
    }

    private fun generateNewFCMToken(email: String, context: Context, senderName: String, message: String, chatId: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { newToken ->
            val firestore = FirebaseFirestore.getInstance()
            val collections = listOf("users", "admins", "doctors")

            fun saveToken(index: Int) {
                if (index >= collections.size) {
                    Log.w("FCM", "Gagal menyimpan token FCM baru untuk $email.")
                    return
                }

                firestore.collection(collections[index])
                    .document(email)
                    .update("fcmToken", newToken)
                    .addOnSuccessListener {
                        Log.d("FCM", "Token FCM baru disimpan untuk $email.")
                        sendFCMRequest(newToken, senderName, message, chatId, System.currentTimeMillis(), context, email)
                    }
                    .addOnFailureListener {
                        saveToken(index + 1)
                    }
            }

            saveToken(0)
        }
    }

    private suspend fun getAccessToken(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val credentials = GoogleCredentials
                    .fromStream(context.assets.open("service-account-key.json"))
                    .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
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


