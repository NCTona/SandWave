package com.tona.sandwave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tona.sandwave.engine.GameEngine
import com.tona.sandwave.ui.App

class MainActivity : ComponentActivity() {

    private lateinit var engine: GameEngine
    private var eventAffect = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cho phép vẽ full màn hình (tràn ra ngoài status/navigation bar)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Ẩn status bar + navigation bar
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        engine = GameEngine(1920f, 1080f, this)

        setContent {
            App(engine, eventAffect)
        }

    }

    override fun onPause() {
        super.onPause()
        // Gọi pause game ở đây
        eventAffect.value = true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            // Mất focus (ví dụ kéo status bar, mở recent apps, ... )
            eventAffect.value = true

        } else {
            // Có focus trở lại
            // Có thể resume hoặc vẫn để user tự resume
        }
    }


}
