package com.example.translator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.translator.databinding.ActivityMainBinding
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// --- Yêu cầu bắt buộc 2: Khai báo biến constant ở vị trí dễ thấy ---
const val GEMINI_API_KEY = "YOUR_API_KEY_HERE"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecording = false
    
    // Khởi tạo Gemini Model
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash", // --- Yêu cầu bắt buộc 1 ---
        apiKey = GEMINI_API_KEY
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupSpeechRecognizer()

        binding.btnRecord.setOnClickListener {
            if (isRecording) {
                stopListening()
            } else {
                checkPermissionAndStart()
            }
        }
    }

    private fun setupSpinner() {
        val languages = listOf("Vietnamese", "English", "French", "Japanese", "Korean", "Chinese")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isRecording = false
                    binding.btnRecord.text = "Start Recording"
                }

                override fun onError(error: Int) {
                    isRecording = false
                    binding.btnRecord.text = "Start Recording"
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        else -> "Speech recognition error: $error"
                    }
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = data?.get(0) ?: ""
                    binding.tvOriginal.text = text
                    if (text.isNotEmpty()) {
                        translateText(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val data = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    binding.tvOriginal.text = data?.get(0) ?: ""
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
        isRecording = true
        binding.btnRecord.text = "Stop Recording"
        binding.tvOriginal.text = "Listening..."
        binding.tvTranslated.text = "Waiting for translation..."
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isRecording = false
        binding.btnRecord.text = "Start Recording"
    }

    private fun translateText(text: String) {
        val targetLang = binding.spinnerLanguage.selectedItem.toString()
        val prompt = "Translate the following text to $targetLang. Only provide the translation, no extra text: $text"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(prompt)
                withContext(Dispatchers.Main) {
                    binding.tvTranslated.text = response.text ?: "No translation found"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvTranslated.text = "Error: ${e.message}"
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}