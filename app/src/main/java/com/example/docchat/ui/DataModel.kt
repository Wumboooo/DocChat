package com.example.docchat.ui

data class Message(
    val senderEmail: String = "",
    val text: String = "",
    val timestamp: Long = 0
)

data class Chat(
    var chatId: String = "",
    var participants: List<String> = emptyList(),
    var lastMessage: String = "",
    var lastUpdated: Long = 0L,
    var status: String = "active", // Status: active, closed
    var summary: Map<String, Any>? = null,   // Summary dari dokter
    var participantName: String? = null, // Nama peserta chat
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
    val name: String = ""
)

data class Doctor(
    var email: String = "",
    val name: String = "",
    val experience: Int = 0,
    val fee: Int = 0,
    val specialization: String = ""
)
