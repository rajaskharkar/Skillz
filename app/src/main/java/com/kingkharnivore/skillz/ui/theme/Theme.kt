package com.kingkharnivore.skillz.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.kingkharnivore.skillz.BuildConfig

val GryffindorRed =  Color(0xFF7F0909)
val RavenclawBlue =  Color(0xFF2F4F6F)
val SteelGrey =  Color(0xFF3A4652)
val SageMist = Color(0xFF7E9B8B)
val SlytherinButNiceTeal = Color(0xFF3F8F8B)
val GryffindorGold = Color(0xFFD3A625)     // Gold
val GryffindorOffWhite = Color(0xFFF2EBDD) // Parchment
val GryffindorBlack = Color(0xFF000000)
val color = if (BuildConfig.SHOW_SCORE) RavenclawBlue else SlytherinButNiceTeal

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val LightGryffindorColors = lightColorScheme(
    primary = color,
    onPrimary = GryffindorOffWhite,

    secondary = GryffindorGold,
    onSecondary = GryffindorBlack,

    background = GryffindorOffWhite,      // #F2EBDD
    onBackground = GryffindorBlack,

    surface = Color(0xFFE4D8BB),          // darker parchment for cards
    onSurface = GryffindorBlack
)

private val DarkGryffindorColors = darkColorScheme(
    primary = color,
    onPrimary = GryffindorOffWhite,

    secondary = GryffindorGold,
    onSecondary = GryffindorBlack,

    background = Color(0xFF1A1412),      // warm dark brown/red-black
    onBackground = Color(0xFFEDEDED),    // soft warm white text

    surface = Color(0xFF221C19),         // lighter, warm card surface
    onSurface = Color(0xFFF5F5F5)        // readable warm white text
)


@Composable
fun SkillzTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) DarkGryffindorColors else LightGryffindorColors
        }

        darkTheme -> DarkGryffindorColors
        else -> LightGryffindorColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}