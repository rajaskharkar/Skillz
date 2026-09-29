package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.domain.achievement.BadgeProgressModel

/** Rows stay lazy even when someone showcases their entire collection. */
internal fun LazyListScope.badgeGalleryRows(
    badges: List<BadgeProgressModel>,
    columns: Int,
    section: String,
    framed: Boolean,
    onOpen: (BadgeProgressModel) -> Unit
) {
    items(badges.chunked(columns), key = { "$section:${it.first().badgeId}" }) { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { badge ->
                key(badge.badgeId) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = if (framed) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                        border = if (framed) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                    ) {
                        EarnedBadgeTile(badge, onOpen)
                    }
                }
            }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun EarnedBadgeTile(badge: BadgeProgressModel, onOpen: (BadgeProgressModel) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(badge) }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BadgeMedallion(badge, BadgeMedallionSize.Medium, showCount = true)
        Text(resolveBadgePresentation(badge.badgeId).title,
            style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}
