package com.example.blindshooter

import android.app.Activity
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var soundPool: SoundPool

    private var gunshotId: Int = 0
    private var reloadId: Int = 0
    private var footstepId: Int = 0

    private val handler = Handler(Looper.getMainLooper())

    private var isEnemyPresent = false
    private var enemyPosition = "none" // "left" or "right"
    private var score = 0
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = FrameLayout(this)
        layout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layout.setBackgroundColor(android.graphics.Color.BLACK)
        setContentView(layout)

        tts = TextToSpeech(this, this)

        // Initialize SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sounds
        gunshotId = soundPool.load(this, R.raw.gunshot, 1)
        reloadId = soundPool.load(this, R.raw.reload, 1)
        footstepId = soundPool.load(this, R.raw.footstep, 1)

        layout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val screenWidth = resources.displayMetrics.widthPixels
                val x = event.x

                if (isPlaying) {
                    val tappedSide = if (x < screenWidth / 2) "left" else "right"
                    handleShot(tappedSide)
                } else {
                    startGame()
                }
            }
            true
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("uz", "UZ") // Try Uzbek, fallback to default if unavailable
            tts.speak("O'yinga xush kelibsiz. Boshlash uchun ekranga bosing.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun startGame() {
        if (isPlaying) return
        isPlaying = true
        score = 0
        soundPool.play(reloadId, 1f, 1f, 1, 0, 1f)
        tts.speak("O'yin boshlandi. Ovoz qaysi tomondan kelsa, o'sha tomonga bosing.", TextToSpeech.QUEUE_FLUSH, null, null)
        scheduleEnemySpawn()
    }

    private fun scheduleEnemySpawn() {
        if (!isPlaying) return
        val delay = Random.nextLong(2000, 5000)
        handler.postDelayed({
            spawnEnemy()
        }, delay)
    }

    private fun spawnEnemy() {
        if (!isPlaying) return
        isEnemyPresent = true
        enemyPosition = if (Random.nextBoolean()) "left" else "right"

        // Use SoundPool for spatial audio (stereo panning)
        val leftVolume = if (enemyPosition == "left") 1.0f else 0.1f
        val rightVolume = if (enemyPosition == "right") 1.0f else 0.1f

        // Play footsteps
        soundPool.play(footstepId, leftVolume, rightVolume, 1, 0, 1f)

        if (enemyPosition == "left") {
            tts.speak("Chapda", TextToSpeech.QUEUE_ADD, null, null)
        } else {
            tts.speak("O'ngda", TextToSpeech.QUEUE_ADD, null, null)
        }

        // Enemy fires if player takes too long
        handler.postDelayed(enemyAction, 2500)
    }

    private val enemyAction = Runnable {
        if (isEnemyPresent) {
            // Enemy shoots player
            soundPool.play(gunshotId, 1f, 1f, 1, 0, 1f)
            tts.speak("Sizni otib qo'yishdi. O'yin tugadi. Sizning ochkongiz: $score. Qaytadan boshlash uchun ekranga bosing.", TextToSpeech.QUEUE_FLUSH, null, null)
            endGame()
        }
    }

    private fun handleShot(tappedSide: String) {
        // Player shoots
        soundPool.play(gunshotId, 1f, 1f, 1, 0, 1f)

        if (isEnemyPresent) {
            handler.removeCallbacks(enemyAction)
            isEnemyPresent = false

            if (tappedSide == enemyPosition) {
                score++
                tts.speak("Tegdi! Yaxshi. Ochko: $score", TextToSpeech.QUEUE_FLUSH, null, null)
                soundPool.play(reloadId, 1f, 1f, 1, 0, 1f)
                scheduleEnemySpawn()
            } else {
                tts.speak("Xato! Dushman boshqa tomonda edi. O'yin tugadi. Ochko: $score", TextToSpeech.QUEUE_FLUSH, null, null)
                endGame()
            }
        } else {
            tts.speak("Bekorga otdingiz.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun endGame() {
        isPlaying = false
        isEnemyPresent = false
    }

    override fun onDestroy() {
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        if (this::soundPool.isInitialized) {
            soundPool.release()
        }
        super.onDestroy()
    }
}
