package com.example.flappyx

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var bird: ImageView
    private lateinit var pipeTop: View
    private lateinit var pipeBottom: View
    private lateinit var scoreText: TextView
    private lateinit var gameLayout: FrameLayout

    private var birdY = 0f
    private var velocity = 0f
    private val gravity = 1.5f
    private val jumpStrength = -25f
    
    private var pipeX = 0f
    private val pipeSpeed = 10f
    private var score = 0
    private var isGameOver = false

    private val handler = Handler(Looper.getMainLooper())
    private val gameLoop = object : Runnable {
        override fun run() {
            if (!isGameOver) {
                updateBird()
                updatePipes()
                checkCollision()
                handler.postDelayed(this, 20) // ~50 FPS
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bird = findViewById(R.id.bird)
        pipeTop = findViewById(R.id.pipe_top)
        pipeBottom = findViewById(R.id.pipe_bottom)
        scoreText = findViewById(R.id.score_text)
        gameLayout = findViewById(R.id.game_layout)

        gameLayout.setOnClickListener {
            if (isGameOver) {
                resetGame()
            } else {
                velocity = jumpStrength
            }
        }

        // Initialize pipe position to the right of the screen
        gameLayout.post {
            pipeX = gameLayout.width.toFloat()
            resetGame()
        }
    }

    private fun updateBird() {
        velocity += gravity
        birdY += velocity
        bird.translationY = birdY

        // Basic boundary check (top and bottom)
        if (bird.y < 0 || bird.y + bird.height > gameLayout.height) {
            gameOver()
        }
    }

    private fun updatePipes() {
        pipeX -= pipeSpeed
        if (pipeX + pipeTop.width < 0) {
            pipeX = gameLayout.width.toFloat()
            score++
            scoreText.text = "Score: $score"
            // Optionally randomize pipe heights here
        }
        pipeTop.translationX = pipeX - gameLayout.width + pipeTop.width
        pipeBottom.translationX = pipeX - gameLayout.width + pipeBottom.width
    }

    private fun checkCollision() {
        // Simple AABB collision detection
        if (isViewOverlapping(bird, pipeTop) || isViewOverlapping(bird, pipeBottom)) {
            gameOver()
        }
    }

    private fun isViewOverlapping(v1: View, v2: View): Boolean {
        val loc1 = IntArray(2)
        v1.getLocationOnScreen(loc1)
        val rect1Left = loc1[0]
        val rect1Top = loc1[1]
        val rect1Right = rect1Left + v1.width
        val rect1Bottom = rect1Top + v1.height

        val loc2 = IntArray(2)
        v2.getLocationOnScreen(loc2)
        val rect2Left = loc2[0]
        val rect2Top = loc2[1]
        val rect2Right = rect2Left + v2.width
        val rect2Bottom = rect2Top + v2.height

        return rect1Left < rect2Right && rect1Right > rect2Left &&
                rect1Top < rect2Bottom && rect1Bottom > rect2Top
    }

    private fun gameOver() {
        isGameOver = true
        scoreText.text = "Game Over! Score: $score\nTap to Restart"
        handler.removeCallbacks(gameLoop)
    }

    private fun resetGame() {
        isGameOver = false
        score = 0
        scoreText.text = "Score: 0"
        birdY = 0f
        velocity = 0f
        pipeX = gameLayout.width.toFloat()
        bird.translationY = 0f
        handler.post(gameLoop)
    }
}
