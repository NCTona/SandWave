package com.tona.balladventure.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tona.balladventure.engine.GameEngine
import com.tona.balladventure.level.Levels
import com.tona.balladventure.type.Types

@Composable
fun MenuScreen(onPlay: () -> Unit, engine: GameEngine) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Box {
                Text(
                    text = "Ball's Adventure",
                    fontSize = 100.sp,
                    color = Color.White,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = Color.White,
                            blurRadius = 8f
                        )
                    )
                )
                Text(
                    text = "Ball's Adventure",
                    fontSize = 100.sp,
                    color = Color.Black
                )
            }


            Box(
                modifier = Modifier
                    .border(4.dp, Color.Black, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box {
                    Text(
                        text = "Tona x 1005",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            shadow = Shadow(
                                color = Color.White,
                                blurRadius = 4f
                            )
                        )
                    )
                    Text(
                        text = "Tona x 1005",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }



            Button(onClick = onPlay,
                modifier = Modifier
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
            ) {
                Text("Play", modifier = Modifier.background(Color.Black.copy(alpha = 0f)))
            }

            // Nút tăng level kiểu vòng
            Button(onClick = {
                val nextLevel = when(engine.state.levelConfig) {
                    Levels.LEVEL_1 -> Levels.LEVEL_2
                    Levels.LEVEL_2 -> Levels.LEVEL_3
                    else -> Levels.LEVEL_1
                }
                engine.state.setLevel(nextLevel)
                engine.importLevelData(nextLevel)

            },
                modifier = Modifier
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
            ) {
                Text("Level: ${
                    when(engine.state.levelConfig) {
                        Levels.LEVEL_1 -> 1
                        Levels.LEVEL_2 -> 2
                        Levels.LEVEL_3 -> 3
                        else -> 1
                    }
                }")
            }

            // Nút chuyển đổi thể loại game
            Button(
                onClick = {
                    val nextType = when (engine.state.gameType) {
                        Types.JUMP -> Types.SWITCH
                        Types.SWITCH -> Types.FLY
                        else -> Types.JUMP
                    }
                    engine.state.setType(nextType)
                    engine.reset()
                },
                modifier = Modifier
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.8f))
            ) {
                Text(
                    text = "Type: ${
                        when (engine.state.gameType) {
                            Types.JUMP -> "Jump"
                            Types.SWITCH -> "Switch"
                            Types.FLY -> "Fly"
                            else -> "Jump"
                        }
                    }"
                )
            }
        }
    }
}
