package com.example.snake.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

class RetroSoundManager(context: Context) {

    private var toneGenerator: ToneGenerator? = null
    var isSoundEnabled: Boolean = true
    var speedProgress: Float = 0f
    var isGameRunning: Boolean = false
        private set

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            Log.w("RetroSoundManager", "ToneGenerator initialization failed", e)
        }
    }

    fun setGameRunning(running: Boolean) {
        isGameRunning = running
    }

    fun updateSpeed(progress: Float) {
        speedProgress = progress.coerceIn(0f, 1f)
    }

    fun playEatSound() {
        if (!isSoundEnabled) return
        try {
            // Speed up tone duration as level speed increases
            val duration = (65 - (speedProgress * 25)).toInt().coerceAtLeast(30)
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_C, duration)
        } catch (_: Exception) {}
    }

    fun playSpecialFoodSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 140)
        } catch (_: Exception) {}
    }

    fun playBonusSound() {
        playSpecialFoodSound()
    }

    fun playBonusSpawnSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_B, 90)
        } catch (_: Exception) {}
    }

    fun playCoinSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 80)
        } catch (_: Exception) {}
    }

    fun playLevelCompleteSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 180)
        } catch (_: Exception) {}
    }

    fun playVictorySound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
        } catch (_: Exception) {}
    }

    fun playTurnSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 20)
        } catch (_: Exception) {}
    }

    fun playGameOverSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 300)
        } catch (_: Exception) {}
    }

    fun playClickSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
