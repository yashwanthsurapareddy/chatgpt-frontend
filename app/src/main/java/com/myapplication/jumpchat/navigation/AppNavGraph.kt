package com.myapplication.jumpchat.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.myapplication.jumpchat.chatMain.ChatScreen
import com.myapplication.jumpchat.localdb.ConversationListScreen
import com.myapplication.jumpchat.voice.VoiceChatScreen
import com.myapplication.jumpchat.voice.VoiceChatViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = "chat/0"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = "chat/{conversationId}?voiceText={voiceText}",
            arguments = listOf(
                navArgument("conversationId") { type = NavType.LongType },
                navArgument("voiceText") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: 0L
            val voiceText = URLDecoder.decode(
                backStackEntry.arguments?.getString("voiceText") ?: "",
                StandardCharsets.UTF_8.name()
            )
            ChatScreen(
                conversationId = conversationId,
                voiceText = voiceText,
                onVoiceClick = { navController.navigate("voice_chat") },
                navController = navController
            )
        }
        composable("convo_list") {
            ConversationListScreen(navController = navController)
        }
        composable(
            "voice_chat/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
        ) { backStackEntry ->
            val convoId = backStackEntry.arguments?.getLong("conversationId") ?: 0L
            VoiceChatScreen(navController, conversationId = convoId)
        }

    }
}
