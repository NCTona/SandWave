package com.tona.sandwave.audio

import android.content.Context
import android.media.MediaPlayer
import com.tona.sandwave.R

class MusicPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    // Bắt đầu phát nhạc nền, mặc định lặp lại
    fun play(resId: Int = R.raw.bg_music) {
        stop() // dừng nhạc cũ nếu có
        mediaPlayer = MediaPlayer.create(context, resId).apply {
            isLooping = true
            setVolume(1.0f, 1.0f)
            start()
        }
    }

    // Tạm dừng
    fun pause() {
        mediaPlayer?.pause()
    }

    // Tiếp tục
    fun resume() {
        mediaPlayer?.start()
    }

    // Dừng hẳn và giải phóng tài nguyên
    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    // Kiểm tra có đang phát hay không
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
}
