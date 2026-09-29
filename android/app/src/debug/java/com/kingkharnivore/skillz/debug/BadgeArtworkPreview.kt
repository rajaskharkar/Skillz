package com.kingkharnivore.skillz.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.ui.screen.shell.inventory.*

@Composable
internal fun BadgeArtworkPreview(dashboard: BadgeDashboard, group: String = "land") {
    val badges = dashboard.badges.filter {
        when(group) {
            "collections" -> it.badgeId.endsWith("_collector") && it.collectionProgress != null
            "mastery" -> it.badgeId.startsWith("mastery_species_creature_")
            else -> LandBadgeCatalog.byId.containsKey(it.badgeId)
        }
    }
    LazyVerticalGrid(columns=GridCells.Fixed(3),contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        items(badges,key={it.badgeId}) { badge ->
            Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.heightIn(min=140.dp)) {
                BadgeMedallion(badge,BadgeMedallionSize.Large)
                Text(resolveBadgePresentation(badge.badgeId).title,style=MaterialTheme.typography.labelMedium)
            }
        }
    }
}
