package com.kingkharnivore.skillz.ui.screen.story

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemUiModel
import com.kingkharnivore.skillz.ui.screen.shell.rooms.ideagrove.IdeaFlowHistorySection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SagaPulseHistorySheet(pulse: IdeaGroveItemUiModel?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(pulse?.title ?: stringResource(R.string.saga_pulse_section_title), style = MaterialTheme.typography.headlineSmall)
            pulse?.journeyName?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            if (pulse?.flows?.isNotEmpty() == true) IdeaFlowHistorySection(pulse.flows)
            else Text(stringResource(R.string.idea_grove_no_flows_from_pulse_yet))
            Button(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
        }
    }
}
