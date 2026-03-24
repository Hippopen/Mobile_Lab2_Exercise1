package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var etInput: EditText
    private lateinit var btnSubmit: Button
    private lateinit var tvResultEmoji: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ánh xạ View
        etInput = findViewById(R.id.etInput)
        btnSubmit = findViewById(R.id.btnSubmit)
        tvResultEmoji = findViewById(R.id.tvResultEmoji)
        progressBar = findViewById(R.id.progressBar)

        // Khởi tạo Gemini Model
        // Lưu ý: Hiện tại chưa có bản 2.5, thông dụng nhất là gemini-1.5-flash
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        btnSubmit.setOnClickListener {
            val inputText = etInput.text.toString().trim()
            if (inputText.isNotEmpty()) {
                analyzeSentiment(generativeModel, inputText)
            } else {
                Toast.makeText(this, "Vui lòng nhập nội dung!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun analyzeSentiment(model: GenerativeModel, text: String) {
        val prompt = "Phân tích cảm xúc của câu tiếng Việt sau đây và chỉ trả về đúng 1 từ khóa duy nhất trong số: HAPPY, NEUTRAL, SAD. Câu cần phân tích: $text"

        // Hiển thị loading
        progressBar.visibility = View.VISIBLE
        tvResultEmoji.text = ""
        btnSubmit.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = model.generateContent(prompt)
                val resultText = response.text?.trim()?.uppercase() ?: ""

                // Hiển thị Emoji tương ứng
                when {
                    resultText.contains("HAPPY") -> tvResultEmoji.text = "😃"
                    resultText.contains("NEUTRAL") -> tvResultEmoji.text = "😐"
                    resultText.contains("SAD") -> tvResultEmoji.text = "😢"
                    else -> tvResultEmoji.text = "❓" // Trường hợp không xác định
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                tvResultEmoji.text = "❌"
            } finally {
                progressBar.visibility = View.GONE
                btnSubmit.isEnabled = true
            }
        }
    }
}
