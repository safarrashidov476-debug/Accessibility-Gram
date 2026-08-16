package com.example.blindshooter

import android.app.Activity
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var toneGenLeft: ToneGenerator
    private lateinit var toneGenRight: ToneGenerator
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
        toneGenLeft = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGenRight = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

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

        // Simulate spatial sound by playing tone mostly on one side
        val volumeLeft = if (enemyPosition == "left") 1.0f else 0.1f
        val volumeRight = if (enemyPosition == "right") 1.0f else 0.1f

        // Warning: ToneGenerator doesn't support stereo panning easily in this basic setup.
        // For a real app, SoundPool with stereo panning is required. We use beep as a placeholder.
        if (enemyPosition == "left") {
            toneGenLeft.startTone(ToneGenerator.TONE_PROP_BEEP, 1000)
            tts.speak("Chapda", TextToSpeech.QUEUE_ADD, null, null)
        } else {
            toneGenRight.startTone(ToneGenerator.TONE_PROP_BEEP2, 1000)
            tts.speak("O'ngda", TextToSpeech.QUEUE_ADD, null, null)
        }

        // Enemy fires if player takes too long
        handler.postDelayed(enemyAction, 2500)
    }

    private val enemyAction = Runnable {
        if (isEnemyPresent) {
            // Enemy shot the player
            tts.speak("Sizni otib qo'yishdi. O'yin tugadi. Sizning ochkongiz: $score. Qaytadan boshlash uchun ekranga bosing.", TextToSpeech.QUEUE_FLUSH, null, null)
            endGame()
        }
    }

    private fun handleShot(tappedSide: String) {
        toneGenLeft.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200) // "Pew" sound

        if (isEnemyPresent) {
            handler.removeCallbacks(enemyAction)
            isEnemyPresent = false

            if (tappedSide == enemyPosition) {
                score++
                tts.speak("Tegdi! Yaxshi. Ochko: $score", TextToSpeech.QUEUE_FLUSH, null, null)
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
        toneGenLeft.release()
        toneGenRight.release()
        super.onDestroy()
    }
}
