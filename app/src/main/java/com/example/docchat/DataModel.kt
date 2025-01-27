package com.example.docchat

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
