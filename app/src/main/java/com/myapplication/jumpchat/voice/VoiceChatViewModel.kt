package com.myapplication.jumpchat.voice

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _finalMessage = MutableStateFlow<String?>(null)
    private val _isMicEnabled = MutableStateFlow(true)
    val isMicEnabled = _isMicEnabled.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun setMicEnabled(enabled: Boolean) {
        _isMicEnabled.value = enabled
    }
    private val _isAIResponding = MutableStateFlow(false)
    val isAIResponding = _isAIResponding.asStateFlow()
    fun setAIResponding(value: Boolean) {
        _isAIResponding.value = value
    }
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

    private var recognizer: SpeechRecognizerManager? = null

    private val _fullTranscript = MutableStateFlow("")
    val fullTranscript = _fullTranscript.asStateFlow()

    private val _currentSentence = MutableStateFlow("")
    val currentSentence = _currentSentence.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _smoothedRmsDb = MutableStateFlow(0f)
    val smoothedRmsDb = _smoothedRmsDb.asStateFlow()

    private var rmsBuffer = 0f
    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId = _activeConversationId.asStateFlow()

    fun setActiveConversationId(id: Long?) {
        _activeConversationId.value = id
    }
    fun initializeRecognizer() {
        if (recognizer != null) return
        recognizer = SpeechRecognizerManager(
            context,
            onResult = { result ->
                viewModelScope.launch {
                    _fullTranscript.value = _fullTranscript.value
                        .plus(" ")
                        .plus(result)
                        .trim()
                    _currentSentence.value = ""
                }
            },
            onPartialResult = { partial ->
                _currentSentence.value = partial
            },
            onErrorCallback = {
                _error.value = it
                _isListening.value = false
            },
            onRmsChangedCallback = { rms ->
                rmsBuffer = rms
                _smoothedRmsDb.value = rms
            }
        )
    }
    fun startListening() {
        _isListening.value = true
        recognizer?.startListening()
    }
    fun stopListening() {
        _isListening.value = false
        recognizer?.stopListening()
    }

    fun resetTranscript() {
        _fullTranscript.value = ""
        _currentSentence.value = ""
    }
    fun destroyRecognizer() {
        recognizer?.destroy()
    }
    fun clearError() {
        _error.value = null
    }
}