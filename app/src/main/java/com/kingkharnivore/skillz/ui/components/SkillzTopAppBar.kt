package com.kingkharnivore.skillz.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.ui.theme.CaveatSemiBold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillzTopAppBar() {
    val title = when (BuildConfig.FLAVOR) {
        "aera" -> "Aera"
        "scyra" -> "Scyra"
        else -> "Skillz"
    }
    TopAppBar(
        title = { Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = CaveatSemiBold,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold
            )
        ) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}