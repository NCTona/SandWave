package com.tona.sandwave.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tona.sandwave.R

@Composable
fun WinGameScreen(onPlayAgain: () -> Unit, onMenu: () -> Unit,onRankScore: () -> Unit, score: Long, coin: Int, distance: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Chỉ hiển thị ảnh
            Image(
                painter = painterResource(id = R.drawable.congratulations), // đổi thành tên ảnh PNG của bạn
                contentDescription = "win icon",
                modifier = Modifier
                    .width(200.dp) // giới hạn chiều ngang
                    .height(64.dp) // hoặc Modifier.size(width, height)
            )

            Text("Your score: $score", fontSize = 28.sp, modifier = Modifier.padding(4.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
            ) {
                Text("Play again", modifier = Modifier.background(Color.Black.copy(alpha = 0f)))
            }

            Button(
                onClick = onRankScore,
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
            ) {
                Text("Rank Score", modifier = Modifier.background(Color.Black.copy(alpha = 0f)))
            }

            Button(
                onClick = onMenu,
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
            ) {
                Text("Main menu", modifier = Modifier.background(Color.Black.copy(alpha = 0f)))
            }
        }
    }
}
