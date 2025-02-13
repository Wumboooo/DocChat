package com.example.docchat.ui

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Messages(
    val senderEmail: String = "",
    val imageUrl: String? = null,
    val text: String = "",
    val timestamp: Long = 0,
    val status: String = "",
    val isRead: Boolean = false
)


data class Chat(
    var chatId: String = "",
    var participants: List<String> = emptyList(),
    var lastMessage: String = "",
    var lastUpdated: Long = 0L,
    var status: String = "active",
    var summary: Map<String, Any>? = null,
    var participantName: String = "",
    var archivedBy: List<String> = emptyList(),
    var lastSenderEmail: String = "",
    var activeParticipants: Map<String, Boolean> = emptyMap(),
    var timestamp: Long = 0L
)

data class ChatSummary(
    var summaryId: String? = null,
    val patientEmail: String = "",
    val doctorName: String = "",
    val date: Long = 0L,
    var disease: String = "",
    var medicine: String = ""
)

data class Admin(
    var email: String = "",
    val name: String = "",
    val tier: String = ""
)

data class Doctor(
    var email: String = "",
    val name: String = "",
    val experience: Int = 0,
    val fee: Int = 0,
    val specialization: String = ""
)

data class UserProfile(
    val name: String = "",
    val profileImage: String = "",
    val phone: String = "",
    val gender: String = "",
    val birthday: String = "",
    val location: String = "",
    val tier: String = "",
    val fcmToken: String = ""
)

