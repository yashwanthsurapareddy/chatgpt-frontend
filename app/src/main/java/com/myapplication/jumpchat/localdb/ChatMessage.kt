package com.myapplication.jumpchat.localdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationId: Long,
    val role: String,
    val content: String,
    val timestamp: Long
)
