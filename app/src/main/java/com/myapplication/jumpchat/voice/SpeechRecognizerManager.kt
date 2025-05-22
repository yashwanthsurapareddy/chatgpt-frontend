package com.myapplication.jumpchat.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechRecognizerManager(
    context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit,
    private val onErrorCallback: (String) -> Unit,
    private val onRmsChangedCallback: (Float) -> Unit
) {
    private var shouldContinueListening = false
    private var isListening = false
    private var isDestroyed = false
    private var lastRmsUpdate = 0L
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 30000)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 30000)
    }
    init {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                val now = System.currentTimeMillis()
                if (now - lastRmsUpdate > 100) {
                    lastRmsUpdate = now
                    onRmsChangedCallback(rmsdB.coerceAtLeast(0f))
                }
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                text?.let {
                    onPartialResult("")
                    onResult(it)
                    restartListening()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                partial?.let { onPartialResult(it) }
            }
            override fun onError(error: Int) {
                if (isDestroyed) return
                isListening = false
                onErrorCallback("Speech Error: $error")
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    fun restartListening() {
        if (isDestroyed) return
        Handler(Looper.getMainLooper()).post {
            recognizer.cancel()
            recognizer.startListening(intent)
        }
    }
    fun startListening() {
        if (isDestroyed || isListening) return
        shouldContinueListening = true
        isListening = true
        try {
            Handler(Looper.getMainLooper()).post {
                recognizer.startListening(intent)
            }
        } catch (e: Exception) {
            onErrorCallback("Start error: ${e.message}")
        }
    }
    fun stopListening() {
        shouldContinueListening = false
        if (!isListening) return
        isListening = false
        try {
            Handler(Looper.getMainLooper()).post {
                recognizer.stopListening()
            }
        } catch (_: Exception) {}
    }
    fun destroy() {
        isDestroyed = true
        try {
            recognizer.destroy()
        } catch (_: Exception) {}
    }
}