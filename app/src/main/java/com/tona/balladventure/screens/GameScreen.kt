package com.tona.balladventure.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tona.balladventure.R
import com.tona.balladventure.engine.GameEngine
import com.tona.balladventure.graphic.GameGraphic
import com.tona.balladventure.thread.GameThread
import com.tona.balladventure.type.Types
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    engine: GameEngine,
    onPause: () -> Unit,
    onGameOver: (Long) -> Unit,
    onPlayAgain: () -> Unit,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
    onLoadedBitmap: () -> Unit
) {

    var isLoaded by remember { mutableStateOf(false) }


    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ---------------- BACKGROUND ----------------
    var background = ImageBitmap.imageResource(id = R.drawable.sandwave_background)
    if (engine.state.gameType == Types.SWITCH){
        background = ImageBitmap.imageResource(id = R.drawable.switch_background)
    } else if (engine.state.gameType == Types.FLY){
        background = ImageBitmap.imageResource(id = R.drawable.fly_background_light)
    }

    // Offset cuộn nền
    var offsetX by remember { mutableStateOf(0f) }

    // Cập nhật offset theo thời gian thật (mượt, không phụ thuộc FPS)
    LaunchedEffect(isPaused) {
        var lastTime = System.nanoTime()
        while (true) {
            if (!isPaused) {
                val currentTime = System.nanoTime()
                val deltaTime = (currentTime - lastTime) / 1_000_000_000f // giây
                lastTime = currentTime

                offsetX -= engine.obstacleSpeed * deltaTime * 60f // chuẩn hóa tốc độ theo 60fps
            }
            delay(16)
        }
    }

    var isMusic by remember { mutableStateOf(false) }
    LaunchedEffect(isMusic) {
        if (isMusic) {
            engine.musicPlayer.play()
        } else {
            engine.musicPlayer.pause()
        }
    }

    var isSound by remember { mutableStateOf(true) }
    LaunchedEffect(isSound) {
        if (isSound) {
            engine.sound.enableSound()
        } else {
            engine.sound.disableSound()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(engine, isPaused) {
                detectTapGestures(
                    onPress = {
                        val gameType = engine.state.gameType.name
                        if (isPaused) return@detectTapGestures

                        when (gameType) {
                            "SWITCH" -> {
                                // Chỉ tap để đổi sóng
                                return@detectTapGestures
                            }



                            "FLY" -> {
                                engine.isHolding = true
                                try {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pointer = event.changes.firstOrNull() ?: break
                                            if (!pointer.pressed) break

                                            // Gửi trực tiếp vị trí Y của tay
                                            engine.playerFly(pointer.position.y)

                                            pointer.consume()
                                        }
                                    }
                                } finally {
                                    engine.isHolding = false
                                }
                            }


                            "JUMP" -> {
                                // Chế độ JUMP bình thường
                                if (engine.state.player.isJumping) {
                                    engine.isHolding = true
                                    val startTime = System.nanoTime()
                                    tryAwaitRelease()
                                    val holdTime = (System.nanoTime() - startTime) / 1_000_000_000f
                                    engine.playerUlti(holdTime)
                                    engine.isHolding = false
                                } else {
                                    engine.isHolding = true
                                    tryAwaitRelease()
                                    engine.playerJump()
                                    engine.isHolding = false
                                }
                            }
                        }
                    },
                    onTap = {
                        if (isPaused || engine.isHolding) return@detectTapGestures
                        val gameType = engine.state.gameType.name
                        when (gameType) {
                            "SWITCH" -> engine.playerSwitch()
                            "FLY" -> { /* tap không làm gì, chỉ giữ mới điều khiển */ }
                            "JUMP" -> engine.playerJump()
                        }
                    }
                )
            }
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Scale ảnh để khớp chiều cao khung
            val scale = canvasHeight / background.height.toFloat()
            val scaledWidth = background.width * scale

            // Làm tròn offset để giảm sai số khi vẽ
            val drawOffset = kotlin.math.floor(offsetX)

            // Vẽ 3 ảnh nối nhau (1 dư phòng để không hở)
            var startX = drawOffset
            while (startX < canvasWidth) {
                drawImage(
                    image = background,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(background.width, background.height),
                    dstOffset = IntOffset(startX.toInt(), 0),
                    dstSize = IntSize(scaledWidth.toInt() + 2, canvasHeight.toInt()) // +2 để bù sai số rounding
                )
                startX += scaledWidth
            }

            // Reset offset sớm khi ảnh đầu ra khỏi màn hình
            if (offsetX <= -scaledWidth) {
                offsetX += scaledWidth
            }
        }



        // Game graphic
        GameGraphic(
            engine = engine,
            modifier = Modifier.fillMaxSize(),
            onLoaded = {
                isLoaded = true
                onLoadedBitmap()
            }
        )

        // Pause button + Score
        if (!isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onPause,
                    modifier = Modifier.align(Alignment.TopEnd),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
                ) {
                    Text("Pause")
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ){
                Box(
                    modifier = Modifier.padding(4.dp)
                ) {
                    // Viền sáng trắng
                    Text(
                        text = "Score: ${engine.state.score}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            shadow = Shadow(
                                color = Color.White,
                                blurRadius = 4f
                            )
                        )
                    )
                    // Lớp text chính
                    Text(
                        text = "Score: ${engine.state.score}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }

                Box(
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "Coin: ${engine.state.coin}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            shadow = Shadow(
                                color = Color.White,
                                blurRadius = 4f
                            )
                        )
                    )
                    Text(
                        text = "Coin: ${engine.state.coin}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }

                Box(
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "Distance: ${engine.state.distance.toLong()}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            shadow = Shadow(
                                color = Color.White,
                                blurRadius = 4f
                            )
                        )
                    )
                    Text(
                        text = "Distance: ${engine.state.distance.toLong()}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }
        }

        // Nút góc dưới trái
        IconButton(
            onClick = { isMusic = !isMusic }, // toggle
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isMusic) R.drawable.music
                    else R.drawable.not_music
                ),
                contentDescription = "Music Toggle",
                tint = Color.Black
            )
        }

        // Nút góc dưới phải
        IconButton(
            onClick = {
                isSound = !isSound
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isSound) R.drawable.baseline_headset_24
                    else R.drawable.not_baseline_headset_24
                ),
                contentDescription = "Music Toggle",
                tint = Color.Black
            )
        }

    }

    // ---------------- GAME THREAD ----------------
    DisposableEffect(engine, isPaused) {
        val gameThread = GameThread(
            engine = engine,
            onGameOver = { onGameOver(engine.state.score) },
            onReset = { onPlayAgain() }
        )

        gameThread.start()
        onDispose { gameThread.stopThread() }
    }
}
