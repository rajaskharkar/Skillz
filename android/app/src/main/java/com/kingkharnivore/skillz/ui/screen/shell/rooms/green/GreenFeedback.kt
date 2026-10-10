package com.kingkharnivore.skillz.ui.screen.shell.rooms.green

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.green.GreenCatalogue
import com.kingkharnivore.skillz.domain.green.PlantGrowthStageResolver
import com.kingkharnivore.skillz.viewmodel.green.GreenUiState

/** The same milestone copy is used in the world and the Chest, including within modal dialogs. */
@Composable
internal fun greenFeedbackText(state: GreenUiState, specimenId: String? = null): String? {
    state.messageRes?.let { return stringResource(it) }
    val action = state.action?.takeIf { it.newLevel != 99 && (specimenId == null || it.specimenId == specimenId) } ?: return null
    val species = state.specimens.firstOrNull { it.id == action.specimenId }?.speciesId?.let(GreenCatalogue.byId::get) ?: return null
    val name = stringResource(species.nameRes)
    return when {
        action.kind == "plant_seed_planted" -> stringResource(R.string.green_taken_root, name)
        action.newLevel == 90 -> stringResource(R.string.green_full_form, name)
        else -> stringResource(R.string.green_growth_feedback, name, action.newLevel,
            stringResource(PlantGrowthStageResolver.resolve(action.newLevel).nameRes))
    }
}

@Composable
internal fun GreenFeedbackMessage(state: GreenUiState, specimenId: String? = null) {
    greenFeedbackText(state, specimenId)?.let {
        Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = if (state.messageRes != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}
