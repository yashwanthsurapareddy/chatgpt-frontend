package com.myapplication.jumpchat.localdb

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChatDatabase.getInstance(application)
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    init {
        loadConversations()
    }
    fun loadConversations() {
        viewModelScope.launch {
            val data = db.conversationDao().getAllConversations()
            _conversations.value = data
            _filteredConversations.value = data
        }
    }
    fun createNewConversation(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = db.conversationDao().insertConversation(Conversation())
            onCreated(id)
        }
    }
    fun deleteConversation(convo: Conversation) {
        viewModelScope.launch {
            db.chatDao().deleteMessagesForConversation(convo.id)
            db.conversationDao().deleteConversation(convo)
            loadConversations()
        }
    }
    fun renameConversation(updated: Conversation) {
        viewModelScope.launch {
            db.conversationDao().updateConversation(updated)
            loadConversations()
        }
    }
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _filteredConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val filteredConversations = _filteredConversations.asStateFlow()
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterConversations()
    }
    fun clearSearch() {
        _searchQuery.value = ""
        filterConversations()
    }
    private fun filterConversations() {
        val query = _searchQuery.value.lowercase()
        _filteredConversations.value = if (query.isBlank()) {
            _conversations.value
        } else {
            _conversations.value.filter {
                it.title.lowercase().contains(query)
            }
        }
    }
    fun refresh() = loadConversations()
}
