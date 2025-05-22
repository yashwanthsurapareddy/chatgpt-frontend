package com.myapplication.jumpchat.voice

import android.Manifest
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.myapplication.jumpchat.gptApi.ChatViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceChatScreen(
    navController: NavController,
    conversationId: Long,
    vm: VoiceChatViewModel = viewModel()
) {
    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val transcript by vm.fullTranscript.collectAsState()
    val currentSentence by vm.currentSentence.collectAsState()
    val isListening by vm.isListening.collectAsState()
    val smoothedRmsDb by vm.smoothedRmsDb.collectAsState()
    val chatViewModel: ChatViewModel = viewModel()
    val context = LocalContext.current
    val tts = remember { android.speech.tts.TextToSpeech(context) { } }
    val isAIResponding by vm.isAIResponding.collectAsState()
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        } else {
            vm.initializeRecognizer()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            vm.destroyRecognizer()
            tts.shutdown()
        }
    }
    LaunchedEffect(conversationId) {
        if (vm.activeConversationId.value == null) {
            vm.setActiveConversationId(conversationId)
        }
    }
    if (!permissionState.status.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Microphone permission is required for voice chat.", color = Color.White)
            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
        return
    }
    val transcriptToSend = ("$transcript $currentSentence").trim()
    val hasText = transcriptToSend.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        CenterAlignedTopAppBar(
            title = {},
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp)
        ) {
            var volumeLevel by remember { mutableStateOf(0.5f) }
            LaunchedEffect(isAIResponding) {
                while (isAIResponding) {
                    volumeLevel = Random.nextFloat() * 0.7f + 0.3f
                    delay(200)
                }
                volumeLevel = 0f
            }
            val animatedVolume by animateFloatAsState(
                targetValue = volumeLevel,
                animationSpec = tween(durationMillis = 200),
                label = "volumeFade"
            )
            MorphingVoiceAnimation(
                isMorphingToWave = !vm.isMicEnabled.collectAsState().value,
                rmsDb = smoothedRmsDb,
                volumeLevel = animatedVolume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }
        val error by vm.error.collectAsState()
        LaunchedEffect(error) {
            if (!error.isNullOrBlank()) {
                delay(5000)
                vm.clearError()
            }
        }
        if (!error.isNullOrBlank()) {
            Text(
                text = "Could not get your voice, restart the mic again",
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )
        }

        Text(
            text = transcriptToSend,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .align(Alignment.CenterHorizontally),
            maxLines = 10
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    vm.activeConversationId.value?.let {
                        navController.navigate("chat/$it") {
                            popUpTo("voice_chat/$it") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.Cyan)
            }

            val isMicEnabled by vm.isMicEnabled.collectAsState()

            IconButton(
                onClick = {
                    if (isListening) vm.stopListening() else vm.startListening()
                },
                enabled = isMicEnabled,
                modifier = Modifier.size(67.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Mic",
                    tint = if (isMicEnabled) Color.Cyan else Color.Gray
                )
            }

            IconButton(
                onClick = {
                    if (hasText) {
                        vm.setAIResponding(true)
                        chatViewModel.sendMessage(
                            content = transcriptToSend,
                            conversationId = vm.activeConversationId.value,
                            onAssistantReply = { convoId ->
                                vm.setActiveConversationId(convoId)
                                vm.setMicEnabled(false)
                                val rawReply = chatViewModel.messages.lastOrNull { it.role == "assistant" }?.content ?: ""
                                val reply = stripMarkdown(rawReply)
                                tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                                    override fun onStart(utteranceId: String?) {}
                                    override fun onDone(utteranceId: String?) {
                                        vm.setMicEnabled(true)
                                        vm.setAIResponding(false)
                                        vm.startListening()
                                    }
                                    override fun onError(utteranceId: String?) {
                                        vm.setMicEnabled(true)
                                        vm.setAIResponding(false)
                                        Toast.makeText(context, "Failed to play voice response.", Toast.LENGTH_SHORT).show()
                                        val reply = stripMarkdown(chatViewModel.messages.lastOrNull { it.role == "assistant" }?.content ?: "")
                                        val retryParams = Bundle().apply {
                                            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                                            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "retry")
                                        }
                                        tts.speak(reply, TextToSpeech.QUEUE_FLUSH, retryParams, "retry")
                                    }

                                })
                                val params = Bundle().apply {
                                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "reply")
                                }
                                tts.speak(reply, TextToSpeech.QUEUE_FLUSH, params, "reply")
                            }
                        )
                        vm.resetTranscript()
                    }
                },
                enabled = hasText && !isAIResponding,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Cyan)
            }
        }
    }
}
@Composable
fun MorphingVoiceAnimation(
    isMorphingToWave: Boolean,
    rmsDb: Float,
    volumeLevel: Float,
    modifier: Modifier = Modifier,
    lineCount: Int = 4,
    yarnColors: List<Color> = listOf(
        Color(0xFF7E57C2),
        Color(0xFF42A5F5),
        Color(0xFFFFB300),
        Color(0xFF66BB6A)
    )
) {
    val segmentCount = 100
    val morphProgress by animateFloatAsState(
        targetValue = if (isMorphingToWave) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "morphProgress"
    )
    val pulse by animateFloatAsState(
        targetValue = (0.9f + rmsDb / 10f).coerceIn(0.8f, 1.6f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pulse"
    )
    val spinSpeed = (30f + rmsDb * 20f).coerceIn(30f, 240f)
    val infiniteTransition = rememberInfiniteTransition(label = "morph_spin_wave")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = (6000 / (spinSpeed / 60)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    val scrollPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scroll_phase"
    )
    val distortionSeeds = remember { List(lineCount) { Random.nextFloat() * 1000f } }
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2, height / 2)
        val baseRadius = size.minDimension / 2.5f
        val yarnRadius = baseRadius * pulse
        val centerY = height / 2f

        for (i in 0 until lineCount) {
            val path = Path()
            val color = yarnColors[i % yarnColors.size]
            val phase = angle + (360f / lineCount) * i
            val osc = 0.15f + 0.07f * i
            val freq = 1.0f + i
            val amp = height * (0.4f + volumeLevel * 0.4f)

            val distortion = distortionSeeds[i]

            for (j in 0 until segmentCount) {
                val t = j.toFloat() / (segmentCount - 1)
                val rad = t * (2 * PI).toFloat()

                val yarnX = center.x + (yarnRadius + osc * baseRadius *
                        sin((freq * rad + phase * PI.toFloat() / 180))) *
                        cos((rad + phase * PI.toFloat() / 180))
                val yarnY = center.y + (yarnRadius + osc * baseRadius *
                        cos((freq * rad + phase * PI.toFloat() / 180))) *
                        sin((rad + phase * PI.toFloat() / 180))

                val waveX = t * width
                val scale = 1f - abs(t - 0.5f) * 2f
                val shapeFactor = 1f + 0.3f * sin(t * 6f + distortion / 40f)
                val waveY = centerY + sin(t * 4f * PI.toFloat() + scrollPhase + distortion / 20f) *
                        amp * shapeFactor * scale

                val x = lerpFloat(yarnX, waveX, morphProgress)
                val y = lerpFloat(yarnY, waveY, morphProgress)

                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
        }
    }
}
private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    (1 - fraction) * start + fraction * stop
fun stripMarkdown(text: String): String {
    return text
        .replace(Regex("""[*_#>]"""), "")
        .replace(Regex("""\[(.*?)]\(.*?\)"""), "$1")
        .replace(Regex("""^(\d+\.\s+)"""), "")
        .replace(Regex("""^- """), "")
}
