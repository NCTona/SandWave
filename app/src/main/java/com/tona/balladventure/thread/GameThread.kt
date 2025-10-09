package com.tona.balladventure.thread

import com.tona.balladventure.engine.GameEngine

class GameThread(
    private val engine: GameEngine,
    private val onGameOver: () -> Unit,
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
                }

                engine.update()

                if (engine.state.isGameOver) {
                    onGameOver()
                    engine.state.isGameOver = false
                    break
                }

                sleep(10) // ~100 FPS
            }
        } catch (e: InterruptedException) {
            // Thread bị ngắt -> thoát an toàn
        }
    }
}
