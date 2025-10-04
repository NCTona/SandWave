package com.tona.sandwave.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RankScoreScreen(
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit,
    score: Long,
    highScores: List<Long>,   // <-- đổi thành danh sách
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                "Leaderboard",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )

            // Hiển thị top 6
            highScores.take(6).forEachIndexed { index, s ->
                Text(
                    text = "${index + 1}. $s",
                    fontSize = 22.sp,
                    fontWeight = if (s == score) FontWeight.Bold else FontWeight.Normal, // in đậm điểm hiện tại
                    textDecoration = if (s == score) TextDecoration.Underline else TextDecoration.None,
                    color = Color.Black,
                    modifier = Modifier.padding(2.dp)
                )
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
