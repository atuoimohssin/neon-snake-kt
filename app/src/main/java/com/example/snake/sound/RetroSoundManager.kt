package com.example.snake.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

class RetroSoundManager(context: Context) {

    private var toneGenerator: ToneGenerator? = null
    var isSoundEnabled: Boolean = true

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            Log.w("RetroSoundManager", "ToneGenerator initialization failed", e)
        }
    }

    fun playEatSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_C, 65)
        } catch (_: Exception) {}
    }

    fun playSpecialFoodSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 140)
        } catch (_: Exception) {}
    }

    fun playBonusSpawnSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_B, 90)
        } catch (_: Exception) {}
    }

    fun playTurnSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 25)
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
