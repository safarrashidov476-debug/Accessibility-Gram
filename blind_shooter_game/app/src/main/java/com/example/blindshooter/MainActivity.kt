package com.example.blindshooter

import android.app.Activity
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.random.Random

class MainActivity : Activity() {

    private lateinit var soundPool: SoundPool

    // SFX
    private var gunshotId: Int = 0
    private var reloadId: Int = 0
    private var footstepId: Int = 0

    // Voice lines (Sardor TTS)
    private var welcomeId: Int = 0
    private var startGameId: Int = 0
    private var leftId: Int = 0
    private var rightId: Int = 0
    private var gameoverId: Int = 0
    private var hitId: Int = 0
    private var missId: Int = 0
    private var emptyShotId: Int = 0
    private var reloadPromptId: Int = 0

    private val handler = Handler(Looper.getMainLooper())

    private var isEnemyPresent = false
    private var enemyPosition = "none" // "left" or "right"
    private var score = 0
    private var isPlaying = false
    private var ammo = 6
    private val maxAmmo = 6

    // Timing tracking
    private var enemyReactionTime = 2500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = FrameLayout(this)
        layout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layout.setBackgroundColor(android.graphics.Color.BLACK)
        setContentView(layout)

        // Initialize SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load SFX
        gunshotId = soundPool.load(this, R.raw.gunshot, 1)
        reloadId = soundPool.load(this, R.raw.reload, 1)
        footstepId = soundPool.load(this, R.raw.footstep, 1)

        // Load Voices
        welcomeId = soundPool.load(this, R.raw.welcome, 1)
        startGameId = soundPool.load(this, R.raw.start_game, 1)
        leftId = soundPool.load(this, R.raw.left, 1)
        rightId = soundPool.load(this, R.raw.right, 1)
        gameoverId = soundPool.load(this, R.raw.gameover, 1)
        hitId = soundPool.load(this, R.raw.hit, 1)
        missId = soundPool.load(this, R.raw.miss, 1)
        emptyShotId = soundPool.load(this, R.raw.empty_shot, 1)
        reloadPromptId = soundPool.load(this, R.raw.reload_prompt, 1)

        // Ensure sounds are loaded before playing welcome
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == welcomeId) {
                // Short delay to allow engine initialization completely
                handler.postDelayed({ playVoice(welcomeId) }, 500)
            }
        }

        // Add Touch Listeners for shooting using GestureDetector to avoid conflict with long press
        val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val screenWidth = resources.displayMetrics.widthPixels
                val x = e.x
                if (isPlaying) {
                    val tappedSide = if (x < screenWidth / 2) "left" else "right"
                    handleShot(tappedSide)
                } else {
                    startGame()
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (isPlaying) {
                    reloadWeapon()
                }
            }
        })

        layout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun playVoice(id: Int, leftVol: Float = 1f, rightVol: Float = 1f) {
        soundPool.play(id, leftVol, rightVol, 1, 0, 1f)
    }

    private fun startGame() {
        if (isPlaying) return
        isPlaying = true
        score = 0
        ammo = maxAmmo
        enemyReactionTime = 2500L // Reset reaction time

        soundPool.play(reloadId, 1f, 1f, 1, 0, 1f)
        handler.postDelayed({ playVoice(startGameId) }, 500)

        // schedule after the voice finishes roughly
        handler.postDelayed({ scheduleEnemySpawn() }, 3000)
    }

    private fun reloadWeapon() {
        ammo = maxAmmo
        soundPool.play(reloadId, 1f, 1f, 1, 0, 1f)
    }

    private fun scheduleEnemySpawn() {
        if (!isPlaying) return
        val delay = Random.nextLong(1500, 3500)
        handler.postDelayed({
            spawnEnemy()
        }, delay)
    }

    private fun spawnEnemy() {
        if (!isPlaying) return
        isEnemyPresent = true
        enemyPosition = if (Random.nextBoolean()) "left" else "right"

        // Difficulty scaling (speed progression)
        // For every 3 points, reaction time decreases by 100ms. Min limit 800ms
        enemyReactionTime = 2500L - ((score / 3) * 150L)
        if (enemyReactionTime < 800L) {
            enemyReactionTime = 800L
        }

        // Enemy variety: normal vs fast
        // Fast enemies have slightly higher pitch footsteps and give you 20% less time to react
        var actualReactionTime = enemyReactionTime
        var footstepRate = 1.0f

        val isFastEnemy = Random.nextBoolean() && score >= 3
        if (isFastEnemy) {
            actualReactionTime = (enemyReactionTime * 0.8).toLong()
            footstepRate = 1.3f
        }

        // Play footsteps with stereo panning
        val leftVolume = if (enemyPosition == "left") 1.0f else 0.1f
        val rightVolume = if (enemyPosition == "right") 1.0f else 0.1f
        soundPool.play(footstepId, leftVolume, rightVolume, 1, 0, footstepRate)

        // Voice cue panning matching the position
        if (enemyPosition == "left") {
            handler.postDelayed({ playVoice(leftId, 1f, 0.1f) }, 200)
        } else {
            handler.postDelayed({ playVoice(rightId, 0.1f, 1f) }, 200)
        }

        // Enemy fires if player takes too long
        handler.postDelayed(enemyAction, actualReactionTime)
    }

    private val enemyAction = Runnable {
        if (isEnemyPresent) {
            // Enemy shoots player
            soundPool.play(gunshotId, 1f, 1f, 1, 0, 1f)
            handler.postDelayed({ playVoice(gameoverId) }, 500)
            endGame()
        }
    }

    private fun handleShot(tappedSide: String) {
        if (ammo <= 0) {
            playVoice(reloadPromptId)
            return
        }

        ammo--
        // Player shoots
        soundPool.play(gunshotId, 1f, 1f, 1, 0, 1f)

        if (isEnemyPresent) {
            handler.removeCallbacks(enemyAction)
            isEnemyPresent = false

            if (tappedSide == enemyPosition) {
                score++
                handler.postDelayed({ playVoice(hitId) }, 300)

                if (ammo == 0) {
                    handler.postDelayed({ playVoice(reloadPromptId) }, 1000)
                }

                scheduleEnemySpawn()
            } else {
                handler.postDelayed({ playVoice(missId) }, 300)
                handler.postDelayed({ playVoice(gameoverId) }, 1500)
                endGame()
            }
        } else {
            handler.postDelayed({ playVoice(emptyShotId) }, 300)
            if (ammo == 0) {
                handler.postDelayed({ playVoice(reloadPromptId) }, 1500)
            }
        }
    }

    private fun endGame() {
        isPlaying = false
        isEnemyPresent = false
    }

    override fun onDestroy() {
        if (this::soundPool.isInitialized) {
            soundPool.release()
        }
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
