package com.example.blindshooter

import android.app.Activity
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : Activity() {

    private lateinit var gestureDetector: GestureDetector
    private var mediaPlayer: MediaPlayer? = null

    private val handler = Handler(Looper.getMainLooper())
    private var currentEnemy: EnemyPosition = EnemyPosition.NONE
    private var isGameOver = false

    enum class EnemyPosition {
        NONE, LEFT, RIGHT, CENTER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playSound(R.raw.start)

        gestureDetector = GestureDetector(this, SwipeGestureListener())

        // Start game loop after initial delay
        handler.postDelayed(gameLoop, 4000)
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (isGameOver) return

            // If there's already an enemy and player didn't react, player crashes
            if (currentEnemy != EnemyPosition.NONE) {
                gameOver()
                return
            }

            spawnEnemy()

            // Give the player 3 seconds to react
            handler.postDelayed(this, 3000)
        }
    }

    private fun spawnEnemy() {
        val random = Random.nextInt(3)
        currentEnemy = when (random) {
            0 -> EnemyPosition.LEFT
            1 -> EnemyPosition.RIGHT
            else -> EnemyPosition.CENTER
        }

        when (currentEnemy) {
            EnemyPosition.LEFT -> playSound(R.raw.enemy_left)
            EnemyPosition.RIGHT -> playSound(R.raw.enemy_right)
            EnemyPosition.CENTER -> playSound(R.raw.enemy_center)
            else -> {}
        }
    }

    private fun gameOver() {
        isGameOver = true
        currentEnemy = EnemyPosition.NONE
        handler.removeCallbacks(gameLoop)
        playSound(R.raw.crash)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun playSound(soundResId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, soundResId)
        mediaPlayer?.start()
    }

    inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {

        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (isGameOver) return true

            if (currentEnemy == EnemyPosition.CENTER) {
                playSound(R.raw.destroyed)
                currentEnemy = EnemyPosition.NONE
                handler.removeCallbacks(gameLoop)
                handler.postDelayed(gameLoop, 2000) // spawn next enemy sooner
            } else {
                playSound(R.raw.shoot)
                // Need a slight delay before crash sound
                handler.postDelayed({ gameOver() }, 1000)
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (isGameOver || e1 == null) return false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x
            if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight()
                    } else {
                        onSwipeLeft()
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun onSwipeRight() {
        // Swiping right dodges an enemy on the left
        if (currentEnemy == EnemyPosition.LEFT) {
            playSound(R.raw.dodge)
            currentEnemy = EnemyPosition.NONE
            handler.removeCallbacks(gameLoop)
            handler.postDelayed(gameLoop, 2000)
        } else {
            playSound(R.raw.turn_right)
            handler.postDelayed({ gameOver() }, 1000)
        }
    }

    private fun onSwipeLeft() {
        // Swiping left dodges an enemy on the right
        if (currentEnemy == EnemyPosition.RIGHT) {
            playSound(R.raw.dodge)
            currentEnemy = EnemyPosition.NONE
            handler.removeCallbacks(gameLoop)
            handler.postDelayed(gameLoop, 2000)
        } else {
            playSound(R.raw.turn_left)
            handler.postDelayed({ gameOver() }, 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(gameLoop)
        mediaPlayer?.release()
    }
}
