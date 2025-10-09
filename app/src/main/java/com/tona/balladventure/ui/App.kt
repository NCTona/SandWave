package com.tona.balladventure.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tona.balladventure.R
import com.tona.balladventure.engine.GameEngine
import com.tona.balladventure.io.GameIO
import com.tona.balladventure.screens.*

@SuppressLint("NewApi")
@Composable
fun App(
    engine: GameEngine,
    eventAffect: MutableState<Boolean>,
) {

    val context = LocalContext.current

    val showMenu = remember { mutableStateOf(true) }
    val showPause = remember { mutableStateOf(false) }
    val showGameOver = remember { mutableStateOf(false) }
    val showRankScore = remember { mutableStateOf(false) }

    var score by remember { mutableStateOf(0L) }

    val isPaused by remember {
        derivedStateOf { engine.state.isPaused }
    }

    val drawable = context.getDrawable(R.mipmap.ic_launcher)
    val bitmap = if (drawable is AdaptiveIconDrawable) {
        val b = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(b)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        b
    } else {
        (drawable as BitmapDrawable).bitmap
    }

    // Animate blur radius (mượt)
    val blurRadius by animateDpAsState(
        targetValue = if (isPaused) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 500)
    )

    // Animate background alpha (mượt)
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPaused) 0.5f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    // **Thêm trạng thái loading**
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(showMenu.value, showPause.value, showGameOver.value, showRankScore.value) {
        engine.state.isPaused = showMenu.value || showPause.value || showGameOver.value || showRankScore.value
    }

    LaunchedEffect(eventAffect) {
        if (eventAffect.value && !showMenu.value && !showGameOver.value) {
            showPause.value = true
            eventAffect.value = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        // GameScreen luôn tồn tại
        GameScreen(
            onPause = { showPause.value = true },
            onGameOver = { sc ->
                score = sc
                GameIO.saveHighScore(context, score)
                showGameOver.value = true
            },
            engine = engine,
            onPlayAgain = {
                showGameOver.value = false
            },
            isPaused = isPaused,
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius),
            // **callback khi load xong**
            onLoadedBitmap = { isLoading = false }
        )

        // ---------- Overlay Loading ----------
        if (isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .border(
                            width = 4.dp,            // độ dày viền
                            color = Color.Black,     // màu viền
                            shape = CircleShape      // hình tròn, hoặc RectangleShape nếu muốn vuông
                        )
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Game Icon",
                        modifier = Modifier.padding(8.dp) // padding để viền không che hình
                    )
                }

                Text(
                    text = "Loading...",
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center).padding(top = 220.dp)
                )
            }
        }

        // Overlay Menu / Pause / GameOver
        if (!isLoading && isPaused) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = overlayAlpha)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    showMenu.value -> MenuScreen(onPlay = {
                        showMenu.value = false
                        showPause.value = false
                    }, engine = engine)
                    showPause.value -> PauseScreen(
                        onContinue = { showPause.value = false },
                        onMenu = {
                            showPause.value = false
                            showMenu.value = true
                            engine.state.isReset = true
                        }
                    )
                    showGameOver.value -> GameOverScreen(
                        onPlayAgain = {
                            showPause.value = false
                            showGameOver.value = false
                            engine.state.isReset = true
                        },
                        onMenu = {
                            showGameOver.value = false
                            showMenu.value = true
                            engine.state.isReset = true
                        },
                        onRankScore = {
                            showGameOver.value = false
                            showMenu.value = false
                            showRankScore.value = true
                        },
                        score = score,
                        highScores = GameIO.getHighScores(context)
                    )
                    showRankScore.value -> RankScoreScreen(
                        onPlayAgain = {
                            showRankScore.value = false
                            engine.state.isReset = true
                        },
                        onMenu = {
                            showRankScore.value = false
                            showMenu.value = true
                            engine.state.isReset = true
                        },
                        score = score,
                        highScores = GameIO.getHighScores(context)
                    )
                }
            }
        }
    }
}

