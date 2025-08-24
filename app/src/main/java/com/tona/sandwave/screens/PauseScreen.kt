package com.tona.sandwave.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PauseScreen(onContinue: () -> Unit, onMenu: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onContinue, modifier = Modifier.padding(8.dp)) {
                Text("Continue")
            }
            Button(onClick = onMenu, modifier = Modifier.padding(8.dp)) {
                Text("Main Menu")
            }
        }
    }
}
