package com.tona.balladventure.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RankScoreScreen(
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit,
    score: Long,
    highScores: List<Long>,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ===== Tiêu đề Leaderboard =====
            Box(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Leaderboard",
                    fontSize = 28.sp,
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
                    text = "Leaderboard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // ===== Hiển thị top 5 =====
            highScores.take(5).forEachIndexed { index, s ->
                Box(modifier = Modifier.padding(2.dp)) {
                    val isCurrent = s == score
                    val rankText = "${index + 1}. $s"

                    // Lớp sáng
                    Text(
                        text = rankText,
                        fontSize = 22.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        textDecoration = if (isCurrent) TextDecoration.Underline else TextDecoration.None,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            shadow = Shadow(
                                color = Color.White,
                                blurRadius = if (isCurrent) 8f else 4f
                            )
                        )
                    )

                    // Lớp chính (màu đen)
                    Text(
                        text = rankText,
                        fontSize = 22.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        textDecoration = if (isCurrent) TextDecoration.Underline else TextDecoration.None,
                        color = Color.Black
                    )
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
            ) {
                Text("Play again")
            }

            Button(
                onClick = onMenu,
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
            ) {
                Text("Main menu")
            }
        }
    }
}
