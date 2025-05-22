package com.myapplication.jumpchat.chatMain

import com.myapplication.jumpchat.gptApi.ChatViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.myapplication.jumpchat.localdb.ChatDatabase
import com.myapplication.jumpchat.localdb.ConversationListScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    voiceText: String = "",
    onVoiceClick: () -> Unit,
    navController: NavController
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val drawerWidth = screenWidth * 0.85f

    val messageText = remember { mutableStateOf(voiceText) }
    val viewModel: ChatViewModel = viewModel()
    val messages = viewModel.messages

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed) {
            focusManager.clearFocus(force = true)
        }
    }
    LaunchedEffect(conversationId) {
        val db = ChatDatabase.getInstance(context)
        val conversation = db.conversationDao().getAllConversations().find { it.id == conversationId }
        if (conversation == null) {
            viewModel.createNewConversation { newId ->
                navController.navigate("chat/$newId") {
                    popUpTo("chat/$conversationId") { inclusive = true }
                }
            }
            return@LaunchedEffect
        }
        viewModel.loadMessages(conversationId)
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(drawerWidth)
                    .background(Color.White)
            ) {
                ConversationListScreen(
                    navController = navController,
                    onConversationClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("chat/$it")
                    }
                )
            }
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("JumpChat", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    modifier = Modifier.shadow(4.dp),
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            bottomBar = {
                BottomChatBar(
                        messageText = messageText.value,
                        onTextChanged = { messageText.value = it },
                    onSendClick = {
                        viewModel.sendMessage(messageText.value, conversationId)
                        messageText.value = ""
                    },
                    onVoiceClick = {
                        navController.navigate("voice_realtime/$conversationId")
                    },
                    navController = navController,
                    conversationId = conversationId
                    )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).background(Color.White)) {
                ChatMessagesList(messages = messages)
            }
        }
    }
}
