package com.myapplication.jumpchat.gptApi

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myapplication.jumpchat.localdb.ChatDatabase
import com.myapplication.jumpchat.localdb.ChatMessage
import com.myapplication.jumpchat.localdb.Conversation
import com.myapplication.jumpchat.localdb.ConversationListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private val db = ChatDatabase.getInstance(context)
    var messages by mutableStateOf(listOf<Message>())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var assistantHasResponded by mutableStateOf(false)
        private set
    fun loadMessages(conversationId: Long) {
        viewModelScope.launch {
            messages = db.chatDao().getMessagesForConversation(conversationId)
                .map { Message(it.role, it.content) }
            assistantHasResponded = messages.any { it.role == "assistant" }
        }
    }
    fun sendMessage(
        content: String,
        conversationId: Long?,
        onAssistantReply: ((Long) -> Unit)? = null
    ) {
        val contentWithFormatInstruction = """
$content

Answer in Markdown format if required. Do not say 'In Markdown' or similar phrases. Just format your answer using proper Markdown syntax like:
- **bold**
- *italic*
- `code`
- bullet points
- numbered lists
- and headings (e.g. ##Title)
""".trimIndent()

        val userMsg = Message("user", content)
        messages = messages + userMsg
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var convoId = conversationId ?: db.conversationDao().insertConversation(Conversation())

            if (convoId != null) {
                db.chatDao().insertMessage(
                    ChatMessage(conversationId = convoId, role = "user", content = content, timestamp = now)
                )
                db.conversationDao().updateConversationTimestamp(convoId, now)
            }

            messages = messages + Message("assistant", "•••")
            isLoading = true

            try {
                val response = RetrofitClient.api.sendChat(
                    apiKey = "Bearer ${ApiKeyProvider.OPENAI_API_KEY}",
                    request = ChatRequest("gpt-3.5-turbo", messages.filter { it.content != "•••" &&  it.role != "assistant"} +
                            Message(role = "user", content = contentWithFormatInstruction)
                    )
                )

                if (response.isSuccessful) {
                    val reply = response.body()?.choices?.firstOrNull()?.message
                    if (reply != null) {
                        messages = messages.dropLast(1)

                        val tokens = reply.content.split(" ")
                        var streamed = ""
                        for (token in tokens) {
                            streamed += if (streamed.isBlank()) token else " $token"
                            messages = messages.dropLastWhile { it.role == "assistant" } + Message("assistant", streamed)
                            delay(150)
                        }

                        if (convoId == null) {
                            convoId = db.conversationDao().insertConversation(Conversation())
                            db.chatDao().insertMessage(
                                ChatMessage(conversationId = convoId!!, role = "user", content = content, timestamp = now)
                            )
                        }

                        db.chatDao().insertMessage(
                            ChatMessage(conversationId = convoId, role = "assistant", content = streamed, timestamp = System.currentTimeMillis())
                        )
                        db.conversationDao().updateConversationTimestamp(convoId!!, System.currentTimeMillis())

                        assistantHasResponded = true
                        maybeGenerateAutoTitle(db, convoId, content)
                        ConversationListViewModel(context).refresh()
                        onAssistantReply?.invoke(convoId)
                    }
                } else {
                    messages = messages.dropLast(1) + Message("assistant", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                messages = messages.dropLast(1) + Message("assistant", "Exception: Check your internet.")
            } finally {
                isLoading = false
            }
        }
    }
    fun createNewConversation(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = db.conversationDao().insertConversation(Conversation())
            onCreated(id)
        }
    }

    private suspend fun maybeGenerateAutoTitle(db: ChatDatabase, convoId: Long, userMessage: String) {
        val convo = db.conversationDao().getAllConversations().find { it.id == convoId } ?: return
        if (convo.title != "New Chat") return

        val titlePrompt = listOf(
            Message("system", "Generate a short, 3-word max title without punctuation."),
            Message("user", userMessage)
        )

        try {
            val titleResponse = RetrofitClient.api.sendChat(
                apiKey = "Bearer ${ApiKeyProvider.OPENAI_API_KEY}",
                request = ChatRequest("gpt-3.5-turbo", titlePrompt)
            )
            val generated = titleResponse.body()?.choices?.firstOrNull()?.message?.content?.trim()?.replace("\"", "")?.take(30)
            if (!generated.isNullOrBlank()) {
                db.conversationDao().updateConversation(
                    convo.copy(title = generated, lastUpdated = System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {}
    }
}
