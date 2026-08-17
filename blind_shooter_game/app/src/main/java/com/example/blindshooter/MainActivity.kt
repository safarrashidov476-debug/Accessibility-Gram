package com.example.blindshooter

import android.app.Activity
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : Activity() {

    private lateinit var gestureDetector: GestureDetector
    private var voicePlayer: MediaPlayer? = null
    private var bgPlayer: MediaPlayer? = null

    private lateinit var soundPool: SoundPool
    private var sfxTakeoff: Int = 0
    private var sfxFlyby: Int = 0
    private var sfxGunshot: Int = 0
    private var sfxExplosion: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private var currentEnemy: EnemyPosition = EnemyPosition.NONE
    private var isGameOver = false
    private var isGameStarted = false

    private var score = 0
    private var spawnDelay = 3000L

    enum class EnemyPosition {
        NONE, LEFT, RIGHT, CENTER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        sfxTakeoff = soundPool.load(this, R.raw.takeoff, 1)
        sfxFlyby = soundPool.load(this, R.raw.enemy_flyby, 1)
        sfxGunshot = soundPool.load(this, R.raw.gunshot, 1)
        sfxExplosion = soundPool.load(this, R.raw.explosion, 1)

        playVoice(R.raw.instructions)

        gestureDetector = GestureDetector(this, SwipeGestureListener())
    }

    override fun onPause() {
        super.onPause()
        // Stop background execution to prevent automatic game over while minimized
        handler.removeCallbacksAndMessages(null)
        if (bgPlayer?.isPlaying == true) {
            bgPlayer?.pause()
        }
        if (voicePlayer?.isPlaying == true) {
            voicePlayer?.pause()
        }
        soundPool.autoPause()
    }

    override fun onResume() {
        super.onResume()
        if (isGameStarted && !isGameOver) {
            bgPlayer?.start()
            // Resume the game loop safely
            handler.postDelayed(gameLoop, 1500)
        }
        soundPool.autoResume()
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (isGameOver) return

            // If there's already an enemy and player didn't react
            if (currentEnemy != EnemyPosition.NONE) {
                gameOver()
                return
            }

            spawnEnemy()

            // Adjust spawn delay based on score to increase adrenaline
            spawnDelay = maxOf(1000L, 3000L - (score * 200L))

            handler.postDelayed(this, spawnDelay)
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
            EnemyPosition.LEFT -> playVoice(R.raw.enemy_left)
            EnemyPosition.RIGHT -> playVoice(R.raw.enemy_right)
            EnemyPosition.CENTER -> playVoice(R.raw.enemy_center)
            else -> {}
        }

        // Add flyby whoosh for immersion when an enemy approaches
        soundPool.play(sfxFlyby, 0.3f, 0.3f, 1, 0, 1.0f)
    }

    private fun gameOver() {
        isGameOver = true
        currentEnemy = EnemyPosition.NONE
        handler.removeCallbacksAndMessages(null)

        bgPlayer?.stop()

        // Big explosion
        soundPool.play(sfxExplosion, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun playVoice(soundResId: Int) {
        voicePlayer?.release()
        voicePlayer = MediaPlayer.create(this, soundResId)
        voicePlayer?.start()
    }

    private fun startBackgroundHum() {
        bgPlayer?.release()
        bgPlayer = MediaPlayer.create(this, R.raw.flight_bg)
        bgPlayer?.isLooping = true
        bgPlayer?.setVolume(0.4f, 0.4f)
        bgPlayer?.start()
    }

    inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {

        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (!isGameStarted || isGameOver) {
                isGameStarted = true
                isGameOver = false
                score = 0
                currentEnemy = EnemyPosition.NONE

                // Play takeoff jet sound
                soundPool.play(sfxTakeoff, 1.0f, 1.0f, 1, 0, 1.0f)
                playVoice(R.raw.start)

                // Start continuous background hum after takeoff finishes
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({
                    if (!isGameOver) startBackgroundHum()
                }, 3500)

                handler.postDelayed(gameLoop, 4000)
                return true
            }

            // Rapid machine gun fire
            soundPool.play(sfxGunshot, 1.0f, 1.0f, 1, 0, 1.0f)
            handler.postDelayed({ soundPool.play(sfxGunshot, 1.0f, 1.0f, 1, 0, 1.0f) }, 100)
            handler.postDelayed({ soundPool.play(sfxGunshot, 1.0f, 1.0f, 1, 0, 1.0f) }, 200)

            if (currentEnemy == EnemyPosition.CENTER) {
                score++
                handler.postDelayed({ soundPool.play(sfxExplosion, 0.7f, 0.7f, 1, 0, 1.0f) }, 250)
                currentEnemy = EnemyPosition.NONE
                handler.removeCallbacks(gameLoop)
                handler.postDelayed(gameLoop, maxOf(1000L, 2000L - (score * 150L)))
            } else {
                handler.postDelayed({ gameOver() }, 500)
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (!isGameStarted || isGameOver || e1 == null) return false

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
        // Dodges enemy on left
        if (currentEnemy == EnemyPosition.LEFT) {
            score++
            // Loud flyby to simulate dodging closely
            soundPool.play(sfxFlyby, 1.0f, 1.0f, 1, 0, 1.2f)
            currentEnemy = EnemyPosition.NONE
            handler.removeCallbacks(gameLoop)
            handler.postDelayed(gameLoop, maxOf(1000L, 2000L - (score * 150L)))
        } else {
            gameOver()
        }
    }

    private fun onSwipeLeft() {
        // Dodges enemy on right
        if (currentEnemy == EnemyPosition.RIGHT) {
            score++
            soundPool.play(sfxFlyby, 1.0f, 1.0f, 1, 0, 1.2f)
            currentEnemy = EnemyPosition.NONE
            handler.removeCallbacks(gameLoop)
            handler.postDelayed(gameLoop, maxOf(1000L, 2000L - (score * 150L)))
        } else {
            gameOver()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        voicePlayer?.release()
        bgPlayer?.release()
        soundPool.release()
    }
}
