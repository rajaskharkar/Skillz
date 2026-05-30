package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneId
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.drawSunlitReefEnvironment
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.drawTheBlueWaterBackground

@Composable
fun TheBlueEmptyOceanPage(
    pageHeight: Dp
) {
    val scheme = MaterialTheme.colorScheme
    val headerDescription = stringResource(R.string.the_blue_header_a11y)
    val transition = rememberInfiniteTransition(label = "the-blue-empty-motion")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
        label = "empty-water-drift"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pageHeight)
            .semantics { contentDescription = headerDescription }
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawTheBlueWaterBackground(TheBlueZoneId.SUNLIT_REEF, scheme, drift)
            drawSunlitReefEnvironment(scheme, drift, animalDensity = 0)
            repeat(12) { index ->
                val x = ((index * 67f + drift * size.width * 0.35f) % (size.width + 60f)) - 30f
                val y = size.height - ((index * 43f + drift * size.height) % size.height)
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.10f + (index % 3) * 0.02f),
                    radius = 3f + (index % 4),
                    center = Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TheBlueOverlaySurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.shell_room_the_blue_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.shell_room_the_blue_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.shell_room_the_blue_empty_body),
                        color = scheme.onSurface.copy(alpha = 0.78f)
                    )
                    Text(
                        text = stringResource(R.string.the_blue_empty_water_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
