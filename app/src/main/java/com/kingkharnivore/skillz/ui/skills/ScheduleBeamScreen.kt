package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBeamScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Beam") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Text("←") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("TODO: date + time + duration + tag picker")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDone) { Text("Done") }
        }
    }
}
