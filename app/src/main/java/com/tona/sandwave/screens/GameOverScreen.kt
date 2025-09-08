package com.tona.sandwave.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameOverScreen(onPlayAgain: () -> Unit, onMenu: () -> Unit, score: Long) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text("Oops!!!", fontSize = 48.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))

            Text("Your score: ${score}", fontSize = 28.sp, modifier = Modifier.padding(8.dp))

            Button(onClick = onPlayAgain,
                modifier = Modifier
                    .padding(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
            ) {
                Text("Play again", modifier = Modifier.background(Color.Black.copy(alpha = 0f)))
            }
            Button(onClick = onMenu,
                modifier = Modifier
                    .padding(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
            ) {
                Text("Main menu", modifier = Modifier.background(Color.Black.copy(alpha = 0f)))
            }
        }
    }
}
