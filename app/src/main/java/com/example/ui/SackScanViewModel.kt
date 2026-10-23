package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.agent.SackScanAgent
import com.example.model.AgentNode
import com.example.model.AgentState
import com.example.model.Language
import com.example.model.SackScanResult
import com.example.model.Translations
import com.example.util.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SackScanViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SackScanViewModel"
    }

    private val agent = SackScanAgent()

    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()

    private val _base64Jpeg = MutableStateFlow<String?>(null)

    private val _optionalHint = MutableStateFlow("")
    val optionalHint: StateFlow<String> = _optionalHint.asStateFlow()

    private val _currentLanguage = MutableStateFlow(Language.ENGLISH)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _agentState = MutableStateFlow<AgentState?>(null)
    val agentState: StateFlow<AgentState?> = _agentState.asStateFlow()

    private val _scanResult = MutableStateFlow<SackScanResult?>(null)
    val scanResult: StateFlow<SackScanResult?> = _scanResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // API key initialized from BuildConfig, but overridable in session
    private val _customApiKey = MutableStateFlow<String?>(null)
    val customApiKey: StateFlow<String?> = _customApiKey.asStateFlow()

    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value?.trim()
        if (!custom.isNullOrEmpty()) {
            return custom
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            buildKey
        } else {
            ""
        }
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key.trim()
        if (key.isNotBlank()) {
            _errorMessage.value = null
        }
    }

    fun setOptionalHint(hint: String) {
        _optionalHint.value = hint
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == Language.ENGLISH) {
            Language.TAMIL
        } else {
            Language.ENGLISH
        }
    }

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
    }

    fun onImageSelected(bitmap: Bitmap) {
        val resized = ImageUtils.resizeToMax1024(bitmap)
        _selectedBitmap.value = resized
        _base64Jpeg.value = ImageUtils.toBase64Jpeg(resized)
        _errorMessage.value = null
        _scanResult.value = null
        _agentState.value = null
    }

    fun onImageUriSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = ImageUtils.loadBitmapFromUri(getApplication(), uri)
                if (bitmap != null) {
                    onImageSelected(bitmap)
                } else {
                    _errorMessage.value = "Failed to load image from gallery. Please try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error reading selected image: ${e.message}"
            }
        }
    }

    fun clearImage() {
        _selectedBitmap.value = null
        _base64Jpeg.value = null
        _scanResult.value = null
        _agentState.value = null
        _errorMessage.value = null
    }

    fun reset() {
        _selectedBitmap.value = null
        _base64Jpeg.value = null
        _optionalHint.value = ""
        _scanResult.value = null
        _agentState.value = null
        _errorMessage.value = null
        _isScanning.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun scanSack() {
        val base64 = _base64Jpeg.value
        val lang = _currentLanguage.value

        if (base64 == null) {
            _errorMessage.value = Translations.noImageSelected(lang)
            return
        }

        val key = getEffectiveApiKey()
        if (key.isBlank()) {
            _errorMessage.value = if (lang == Language.TAMIL) {
                "ஜெமினி API சாவி தேவை. மேலே உள்ள சாவி ஐகானைத் தட்டி சாவியை உள்ளிடவும் அல்லது AI Studio Secrets பலகத்தில் உள்ளமைக்கவும்."
            } else {
                "Gemini API key is required. Tap the Key icon (🔑) in the header to enter your API key, or set GEMINI_API_KEY in AI Studio Secrets."
            }
            return
        }

        _isScanning.value = true
        _errorMessage.value = null
        _scanResult.value = null

        val initialState = AgentState(
            bitmap = _selectedBitmap.value,
            base64Image = base64,
            optionalHint = _optionalHint.value,
            language = lang,
            currentNode = AgentNode.IDLE,
            stepLogs = agent.createInitialStepLogs(lang)
        )
        _agentState.value = initialState

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting SackScan Agent execution...")
                println("SackScanAI: Starting SackScan Agent execution...")

                val finalState = agent.execute(initialState, key) { intermediateState ->
                    _agentState.value = intermediateState
                }

                _agentState.value = finalState

                if (finalState.currentNode == AgentNode.FAILED) {
                    _errorMessage.value = finalState.errorMessage ?: "Analysis failed"
                    Log.e(TAG, "Agent finished with failure: ${finalState.errorMessage}")
                } else {
                    _scanResult.value = finalState.result
                    Log.d(TAG, "Agent finished successfully with result: ${finalState.result?.estimatedWeightKg} kg")
                }
            } catch (t: Throwable) {
                val err = "Unexpected error: ${t.localizedMessage ?: t.message}"
                Log.e(TAG, err, t)
                _errorMessage.value = err
            } finally {
                _isScanning.value = false
            }
        }
    }
}
