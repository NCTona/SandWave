package com.tona.sandwave.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.tona.sandwave.R

class Sound(context: Context) {
    private val soundPool: SoundPool
    private val jumpSoundId: Int
    private val crashSoundId: Int
    private val fireSoundId: Int
    private val shockwaveSoundId: Int
    private val coinCollectedSoundId: Int

    // Biến kiểm soát tắt/bật âm thanh
    private var isSoundEnabled: Boolean = true

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(2)
            .build()

        fireSoundId = soundPool.load(context, R.raw.fire, 1)
        jumpSoundId = soundPool.load(context, R.raw.jump, 1)
        crashSoundId = soundPool.load(context, R.raw.crash, 1)
        shockwaveSoundId = soundPool.load(context, R.raw.explosion, 1)
        coinCollectedSoundId = soundPool.load(context, R.raw.gold_collect, 1)
    }

    fun playJumpSound() {
        if (isSoundEnabled) {
            soundPool.play(jumpSoundId, 0.5f, 0.5f, 1, 0, 1f)
        }
    }

    fun playCrashSound() {
        if (isSoundEnabled) {
            soundPool.play(crashSoundId, 0.5f, 0.5f, 1, 0, 1f)
        }
    }

    fun playFireSound() {
        if (isSoundEnabled) {
            soundPool.play(fireSoundId, 0.1f, 0.1f, 1, 0, 1.5f)
        }
    }

    fun playShockwaveSound() {
        if (isSoundEnabled) {
            soundPool.play(shockwaveSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playCollectCoinSound() {
        if (isSoundEnabled) {
            soundPool.play(coinCollectedSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    // Bật âm thanh
    fun enableSound() {
        isSoundEnabled = true
    }

    // Tắt âm thanh
    fun disableSound() {
        isSoundEnabled = false
    }

    // Kiểm tra trạng thái
    fun isSoundOn(): Boolean = isSoundEnabled

    fun release() {
        soundPool.release()
    }
}
