package com.example.translator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
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

// --- Yêu cầu bắt buộc 2: Khai báo biến constant ở vị trí dễ thấy nhất ---
const val GEMINI_API_KEY = "AIzaSyCvDCSYsHHxRD3S2DXFSPJWIepeZxzm_jA"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecording = false
    
    // Khởi tạo Gemini Model - Yêu cầu bắt buộc 1: gemini-2.5-flash
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = GEMINI_API_KEY
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupSpeechRecognizer()

        // Xử lý sự kiện khi gõ văn bản và nhấn ENTER
        binding.tvOriginal.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                
                val text = binding.tvOriginal.text.toString().trim()
                if (text.isNotEmpty() && text != "Listening..." && text != "Processing...") {
                    translateText(text)
                }
                true // Chặn không cho xuống dòng
            } else {
                false
            }
        }

        // Click bình thường để Ghi âm/Dừng (Dùng cho máy thật)
        binding.btnRecord.setOnClickListener {
            if (isRecording) {
                stopListening()
            } else {
                checkPermissionAndStart()
            }
        }

        // --- CƠ CHẾ TEST TRÊN EMULATOR ---
        binding.btnRecord.setOnLongClickListener {
            val mockSpeech = "Hello, I am testing the speech translation app using Gemini AI."
            binding.tvOriginal.setText(mockSpeech)
            binding.tvTranslated.text = "Translating..."
            translateText(mockSpeech)
            Toast.makeText(this, "Emulator Test: Mocking speech input", Toast.LENGTH_SHORT).show()
            true
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
                    if (binding.tvOriginal.text.toString() == "Listening...") {
                        binding.tvOriginal.setText("Processing...")
                    }
                }

                override fun onError(error: Int) {
                    isRecording = false
                    binding.btnRecord.text = "Start Recording"
                    
                    val currentText = binding.tvOriginal.text.toString()
                    if (currentText == "Listening..." || currentText == "Processing...") {
                        binding.tvOriginal.setText("Speech will appear here...")
                    }

                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                        else -> "Speech recognition error: $error"
                    }
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = data?.get(0) ?: ""
                    if (text.isNotEmpty()) {
                        binding.tvOriginal.setText(text)
                        translateText(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val data = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partialText = data?.get(0)
                    if (!partialText.isNullOrEmpty()) {
                        binding.tvOriginal.setText(partialText)
                    }
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
        try {
            speechRecognizer?.startListening(intent)
            isRecording = true
            binding.btnRecord.text = "Stop Recording"
            binding.tvOriginal.setText("Listening...")
            binding.tvTranslated.text = "Waiting for translation..."
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition failed to start", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isRecording = false
        binding.btnRecord.text = "Start Recording"
        if (binding.tvOriginal.text.toString() == "Listening...") {
            binding.tvOriginal.setText("Processing...")
        }
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