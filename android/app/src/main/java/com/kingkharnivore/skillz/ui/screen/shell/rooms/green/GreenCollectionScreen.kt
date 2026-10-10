package com.kingkharnivore.skillz.ui.screen.shell.rooms.green

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.green.*
import com.kingkharnivore.skillz.viewmodel.green.GreenUiState

/** The Chest owns specimen browsing; it uses the same detail and watering controls as the world. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GreenCollectionScreen(state: GreenUiState, onWater: (String, Int, String) -> Unit,
    onDismissFeedback: () -> Unit, onOpenGreen: () -> Unit, focusSpeciesId: String? = null, focusSpecimenId: String? = null) {
    var environment by rememberSaveable { mutableStateOf(GreenCatalogue.byId[focusSpeciesId]?.environment ?: GreenEnvironment.GARDEN) }
    var specimenId by rememberSaveable { mutableStateOf<String?>(null) }
    val plants = state.specimens.filter { GreenCatalogue.byId[it.speciesId]?.environment == environment }
    var focused by rememberSaveable(focusSpeciesId, focusSpecimenId) { mutableStateOf(false) }
    LaunchedEffect(focusSpeciesId, focusSpecimenId, state.specimens) {
        if (focused) return@LaunchedEffect
        val exact = state.specimens.firstOrNull { it.id == focusSpecimenId }
        (exact?.speciesId ?: focusSpeciesId)?.let { species ->
            GreenCatalogue.byId[species]?.let { environment = it.environment }
            specimenId = exact?.id ?: state.specimens.filter { it.speciesId == species }.maxWithOrNull(
                compareBy<com.kingkharnivore.skillz.data.model.entity.green.PlantSpecimenEntity> { it.level < 99 }.thenBy { it.level })?.id
            focused = specimenId != null
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val mastery = state.action?.takeIf { it.newLevel == 99 }?.let { action ->
        state.specimens.firstOrNull { it.id == action.specimenId }
    }
    val feedback = greenFeedbackText(state)
    val modalVisible = specimenId != null || mastery != null
    LaunchedEffect(state.feedbackId, state.action?.id, state.messageRes, feedback, modalVisible) {
        if (!modalVisible && feedback != null) { snackbar.showSnackbar(feedback); onDismissFeedback() }
    }
    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.green_drop_balance, state.drops), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GreenEnvironment.entries.forEach { env ->
                FilterChip(environment == env, { environment = env }, { Text(stringResource(env.nameRes)) }, colors = greenSecondarySelectionColors())
            }
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                item { EnvironmentSummary(environment, plants) }
                if (plants.isEmpty()) item { Text(stringResource(R.string.green_empty)) }
                item { TextButton(onClick = onOpenGreen) { Text(stringResource(R.string.green_open_room)) } }
                items(plants, key = { it.id }) { plant -> SpecimenCard(plant) { specimenId = plant.id } }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    if (mastery == null) state.specimens.firstOrNull { it.id == specimenId }?.let { plant ->
        SpecimenDialog(plant, state, onWater) { specimenId = null; onDismissFeedback() }
    }
    mastery?.let { plant ->
        MasteryDialog(plant, state, { specimenId = null; onDismissFeedback() }, R.string.green_return_chest)
    }
}
