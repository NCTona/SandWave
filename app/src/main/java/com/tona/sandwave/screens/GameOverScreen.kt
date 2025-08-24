package com.tona.sandwave.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GameOverScreen(onPlayAgain: () -> Unit, onMenu: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onPlayAgain, modifier = Modifier.padding(8.dp)) {
                Text("Play Again")
            }
            Button(onClick = onMenu, modifier = Modifier.padding(8.dp)) {
                Text("Main Menu")
            }
        }
    }
}
