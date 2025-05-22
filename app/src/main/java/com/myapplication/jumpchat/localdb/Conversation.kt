package com.myapplication.jumpchat.localdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "New Chat",
    val lastUpdated: Long = System.currentTimeMillis()
)
