package com.tona.sandwave.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreen(onPlay: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Text("SandWave", color = Color.Black, fontSize = 100.sp)

            Button(onClick = onPlay,
                modifier = Modifier
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
            ) {
                Text("Play", modifier = Modifier.background(Color.Black.copy(alpha = 0f)))
            }

        }
    }
}
