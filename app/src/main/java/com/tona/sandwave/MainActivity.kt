package com.tona.sandwave

import android.os.Bundle
import android.view.WindowManager
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

        // Cho phép layout tràn toàn màn hình, kể cả notch
        window.apply {
            WindowCompat.setDecorFitsSystemWindows(this, false)
            attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Ẩn status bar + navigation bar
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        engine = GameEngine(screenWidth, screenHeight, this)

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
