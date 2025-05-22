package com.myapplication.jumpchat.chatMain

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
@Composable
fun MarkdownText(content: String, modifier: Modifier = Modifier) {
    val annotated = buildAnnotatedString {
        val lines = content.lines()
        lines.forEachIndexed { index, line ->
            when {
                line.startsWith("### ") -> {
                    pushStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold))
                    append(line.removePrefix("### ").trim())
                    pop()
                }
                line.startsWith("## ") -> {
                    pushStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                    append(line.removePrefix("## ").trim())
                    pop()
                }
                line.startsWith("# ") -> {
                    pushStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold))
                    append(line.removePrefix("# ").trim())
                    pop()
                }
                line.trimStart().startsWith("- ") -> {
                    append("• ")
                    this@buildAnnotatedString.parseInlineMarkdown(line.trimStart().removePrefix("- "))
                }
                line.trimStart().matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val trimmed = line.trimStart()
                    val numberPart = trimmed.substringBefore(".").trim()
                    val rest = trimmed.substringAfter(".").trim()
                    append("$numberPart. ")
                    this@buildAnnotatedString.parseInlineMarkdown(rest)
                }
                else -> {
                    this@buildAnnotatedString.parseInlineMarkdown(line)
                }
            }
            if (index != lines.lastIndex) append("\n")
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
        color = Color.Black,
        modifier = modifier
    )
}
fun AnnotatedString.Builder.parseInlineMarkdown(text: String) {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(i + 2, end))
                    pop()
                    i = end + 2
                } else {
                    append("**")
                    i += 2
                }
            }
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append("*")
                    i += 1
                }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray
                        )
                    )
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append("`")
                    i += 1
                }
            }
            else -> {
                val nextSpecial = listOf(
                    text.indexOf("**", i).takeIf { it != -1 } ?: text.length,
                    text.indexOf("*", i).takeIf { it != -1 } ?: text.length,
                    text.indexOf("`", i).takeIf { it != -1 } ?: text.length
                ).minOrNull() ?: text.length

                append(text.substring(i, nextSpecial))
                i = nextSpecial
            }
        }
    }
}