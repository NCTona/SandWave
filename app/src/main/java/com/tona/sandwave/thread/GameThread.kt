package com.tona.sandwave.thread

import com.tona.sandwave.engine.GameEngine

class GameThread(
    private val engine: GameEngine,
    private val onGameOver: () -> Unit,
    private val isPausedProvider: () -> Boolean,
    private val onReset: (() -> Unit)? = null
) : Thread() {

    @Volatile
    private var running = true
    @Volatile
    private var needReset = false

    fun stopThread() {
        running = false
        interrupt() // đảm bảo thoát khỏi sleep
    }

    fun requestReset() {
        needReset = true
    }

    override fun run() {
        try {
            while (running && !isInterrupted) {
                if (needReset) {
                    engine.reset()
                    onReset?.invoke()
                    needReset = false
                }

                if (!isPausedProvider()) {
                    engine.update()

                    if (engine.state.isGameOver) {
                        onGameOver()
                        engine.state.isGameOver = false
                        break
                    }
                }
                sleep(8) // ~120 FPS
            }
        } catch (e: InterruptedException) {
            // Thread bị ngắt -> thoát an toàn
        }
    }
}
