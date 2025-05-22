package com.myapplication.jumpchat.chatMain

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.myapplication.jumpchat.gptApi.Message
import java.util.*

@Composable
fun ChatMessagesList(messages: List<Message>) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val tts = remember { TextToSpeech(context, null) }
    var speakingMessageId by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    LaunchedEffect(Unit) {
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { speakingMessageId = null }
            override fun onError(utteranceId: String?) { speakingMessageId = null }
        })
    }
    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        itemsIndexed(messages.reversed()) { index, message ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (message.role == "assistant") 3.dp else 0.dp,
                        end = if (message.role == "user") 2.dp else 0.dp,
                        top = 6.dp,
                        bottom = 6.dp
                    ),
                horizontalAlignment = if (message.role == "user") Alignment.End else Alignment.Start
            ) {
                if (message.role == "assistant" && message.content == "•••") {
                    ShimmeringThinkingText()
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(top = 2.3.dp, end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .widthIn(min = 40.dp, max = 280.dp)
                                .background(
                                    color = if (message.role == "user") Color(0xFFDCF8C6) else Color(0xFFEFEFEF),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            if (message.role == "assistant") {
                                val animatedAlpha by animateFloatAsState(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 200)
                                )

                                Box(modifier = Modifier.alpha(animatedAlpha)) {
                                    MarkdownText(message.content)
                                }
                            }
                            else {
                                Text(text = message.content)
                            }
                        }
                        if (message.role == "assistant") {
                            Row(
                                modifier = Modifier
                                    .padding(top = 2.dp, end = 6.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(message.content))
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (speakingMessageId == index) {
                                            tts.stop()
                                            speakingMessageId = null
                                        } else {
                                            speakingMessageId = index
                                            tts.language = Locale.US
                                            val params = Bundle().apply {
                                                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "$index")
                                            }
                                            val spokenText = stripMarkdown(message.content)
                                            tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, params, "$index")

                                        }
                                    },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = if (speakingMessageId == index) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (speakingMessageId == index) "Pause" else "Play",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(0)
    }
}
fun stripMarkdown(text: String): String {
    return text
        .replace(Regex("""[*_`#>]"""), "")
        .replace(Regex("""\[(.*?)]\(.*?\)"""), "$1")
        .replace(Regex("""^(\d+\.\s+)"""), "")
        .replace(Regex("""^- """), "")
}

