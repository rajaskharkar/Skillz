package com.kingkharnivore.skillz.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import com.kingkharnivore.skillz.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillzTopAppBar() {
    val title = when (BuildConfig.FLAVOR) {
        "aera" -> "Aera"
        "scyra" -> "Scyra"
        else -> "Skillz"
    }
    TopAppBar(
        title = { Text(title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}