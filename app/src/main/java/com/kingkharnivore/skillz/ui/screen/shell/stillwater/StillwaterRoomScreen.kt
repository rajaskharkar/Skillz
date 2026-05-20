package com.kingkharnivore.skillz.ui.screen.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.data.model.shell.*
import com.kingkharnivore.skillz.domain.shell.*
import com.kingkharnivore.skillz.viewmodel.shell.*
import kotlinx.coroutines.*
import kotlin.math.*


@Composable
internal fun StillwaterRoomScreen(
    uiState: ShellUiState,
    onPerspective: (StillwaterPerspective) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoomHeader(
            title = R.string.shell_room_stillwater_title,
            body = R.string.shell_stillwater_body
        )

        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = scheme.primary
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(shellChamberBrush())
            ) {
                TurtleShellInteriorBackground(
                    modifier = Modifier.matchParentSize(),
                    centerGlow = true
                )

                Canvas(Modifier.matchParentSize()) {
                    repeat(6) { i ->
                        drawCircle(
                            color = scheme.primary.copy(alpha = 0.14f),
                            radius = 44f + i * 18f,
                            center = Offset(size.width / 2, size.height / 2),
                            style = Stroke(3f)
                        )
                    }
                }

                Text(
                    text = displayStillwater(uiState.stillwaterTotal, uiState.perspective),
                    color = scheme.onPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.24f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.shell_view_as),
                    color = scheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                val selectorDescription = stringResource(R.string.shell_stillwater_selector_a11y)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .semantics {
                            contentDescription = selectorDescription
                        }
                ) {
                    StillwaterPerspective.entries.forEach { perspective ->
                        FilterChip(
                            selected = uiState.perspective == perspective,
                            onClick = { onPerspective(perspective) },
                            label = { Text(stringResource(labelFor(perspective))) }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.shell_stillwater_same_water),
                    color = scheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = stringResource(R.string.shell_soft_flow_copy),
                    color = scheme.onSurface.copy(alpha = 0.76f)
                )
            }
        }
    }
}



@Composable