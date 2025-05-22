package com.myapplication.jumpchat.chatMain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun BottomChatBar(
    messageText: String,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onVoiceClick: () -> Unit,
    navController: NavController,
    conversationId: Long
){
    Row(modifier = Modifier
        .fillMaxWidth()
        .background(color = Color.White)
        .padding(horizontal = 12.dp, vertical = 4.dp)
        .navigationBarsPadding(),
        verticalAlignment = Alignment.Bottom){
        IconButton(onClick = {
            navController.navigate("voice_chat/$conversationId")
        },
            modifier = Modifier.size(40.dp).padding(bottom = 10.dp)) {
            Icon(Icons.Default.GraphicEq, contentDescription = "voice")
        }
            TextField(
                value = messageText,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 7.dp)
                    .heightIn(min = 56.dp, max = 272.dp)
                    .border(width = 0.5.dp, color = Color.LightGray, shape =RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                placeholder = { Text("Type a message...") },
                maxLines = 25,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFFFFFFF),
                    unfocusedContainerColor = Color(0xFFFFFFFF),
                    disabledContainerColor = Color(0xFFFFFFFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        IconButton(
            onClick = onSendClick,
            enabled = messageText.isNotBlank(),
            modifier = Modifier.size(40.dp).padding(bottom = 12.dp)
        ){
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}