package com.tona.sandwave.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.tona.sandwave.R

class Sound(context: Context) {
    private val soundPool: SoundPool
    private val jumpSoundId: Int
    private val crashSoundId: Int

    init {
        // Khởi tạo SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(2) // Số luồng âm thanh đồng thời
            .build()

        // Load âm thanh từ res/raw
        jumpSoundId = soundPool.load(context, R.raw.jump, 1)
        crashSoundId = soundPool.load(context, R.raw.crash, 1)
    }

    // Phát âm thanh nhảy
    fun playJumpSound() {
        soundPool.play(jumpSoundId, 1f, 1f, 1, 0, 1f)
    }

    // Phát âm thanh va chạm
    fun playCrashSound() {
        soundPool.play(crashSoundId, 1f, 1f, 1, 0, 1f)
    }

    // Giải phóng tài nguyên
    fun release() {
        soundPool.release()
    }
}