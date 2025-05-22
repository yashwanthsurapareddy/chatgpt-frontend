package com.myapplication.jumpchat.localdb

import androidx.room.*

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY lastUpdated DESC")
    suspend fun getAllConversations(): List<Conversation>

    @Insert
    suspend fun insertConversation(convo: Conversation): Long

    @Update
    suspend fun updateConversation(convo: Conversation)

    @Delete
    suspend fun deleteConversation(convo: Conversation)

    @Query("UPDATE conversations SET lastUpdated = :timestamp WHERE id = :convoId")
    suspend fun updateConversationTimestamp(convoId: Long, timestamp: Long)

}
