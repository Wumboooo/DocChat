package com.example.docchat

data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0
)

data class Chat(
    val chatId: String = "",
    val participants: List<String> = listOf(),
    val lastMessage: String = "",
    val timestamp: Long = 0L
)