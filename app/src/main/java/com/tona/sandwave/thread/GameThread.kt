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

    fun stopThread() {
        running = false
        interrupt() // đảm bảo thoát khỏi sleep
    }

    override fun run() {
        try {
            while (running && !isInterrupted) {
                if (engine.state.isReset) {
                    engine.reset()
                    onReset?.invoke()
                }

                if (!isPausedProvider()) {
                    engine.update()

                    if (engine.state.isGameOver) {
                        onGameOver()
                        engine.state.isGameOver = false
                        break
                    }
                }
                sleep(10) // ~120 FPS
            }
        } catch (e: InterruptedException) {
            // Thread bị ngắt -> thoát an toàn
        }
    }
}
