package com.myapplication.jumpchat.chatMain

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset

@Composable
fun ShimmeringThinkingText(
    text: String = "Thinking...",
    fontSize: TextUnit = TextUnit.Unspecified,
    style: TextStyle = TextStyle.Default
) {
    val shimmerColors = listOf(
        Color.Black.copy(alpha = 0.3f),
        Color.Black,
        Color.Black.copy(alpha = 0.3f)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 200f, 0f),
        end = Offset(translateAnim.value, 0f)
    )
    Text(
        text = text,
        fontSize = fontSize,
        style = style.copy(brush = brush)
    )
}
