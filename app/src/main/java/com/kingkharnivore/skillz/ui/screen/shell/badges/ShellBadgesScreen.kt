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
internal fun BadgesScreen(uiState: ShellUiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            RoomHeader(
                title = R.string.shell_badges_title,
                body = R.string.shell_badges_body
            )
        }

        items(uiState.badges) { badge ->
            val def = ShellContentCatalog.badge(badge.badgeId) ?: return@items
            val title = stringResource(def.titleRes)
            val badgeDescription = stringResource(R.string.shell_badge_a11y, title, badge.count)

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.semantics {
                    contentDescription = badgeDescription
                }
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.MilitaryTech,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.shell_badge_row_title, title, badge.count)) },
                    supportingContent = {
                        Text(stringResource(def.descriptionRes))
                    }
                )
            }
        }
    }
}

@Composable