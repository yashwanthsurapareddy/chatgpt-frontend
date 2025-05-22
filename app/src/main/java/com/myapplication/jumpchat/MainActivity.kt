package com.myapplication.jumpchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.myapplication.jumpchat.localdb.ChatDatabase
import com.myapplication.jumpchat.localdb.Conversation
import com.myapplication.jumpchat.navigation.AppNavGraph
import com.myapplication.jumpchat.navigation.AppNavigation
import com.myapplication.jumpchat.ui.theme.JumpChatTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JumpChatTheme {
                var initialDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val db = ChatDatabase.getInstance(applicationContext)
                    val existing = db.conversationDao().getAllConversations()
                    initialDestination = if (existing.isNotEmpty()) {
                        "chat/${existing.first().id}?voiceText="
                    } else {
                        "chat/0?voiceText="
                    }
                }
                if (initialDestination != null) {
                    AppNavigation(startDestination = initialDestination!!)
                }
            }
        }
    }
}
