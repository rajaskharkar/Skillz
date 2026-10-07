package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.chronicle.ChroniclePage
import com.kingkharnivore.skillz.ui.screen.chronicle.ChronicleStateHolder
import kotlinx.coroutines.launch

/** Metadata is saved explicitly; chronicle actions persist as they do during a Flow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEntryEditSheet(
    ownerType: String,
    ownerId: Long,
    title: String,
    tagName: String,
    createEditor: (String, String) -> ChronicleStateHolder,
    onSave: suspend (String, Long, String, String) -> Unit,
    onClose: () -> Unit,
    extraDetails: @Composable () -> Unit = {}
) {
    val holder = remember(ownerType, ownerId) { createEditor(ownerType, ownerId.toString()) }
    DisposableEffect(holder) { onDispose { holder.close() } }
    val state by holder.state.collectAsState()
    var editTitle by rememberSaveable(ownerType, ownerId) { mutableStateOf(title) }
    var editTag by rememberSaveable(ownerType, ownerId) { mutableStateOf(tagName) }
    var page by rememberSaveable(ownerType, ownerId) { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    var unfinished by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isFlow = ownerType == "SESSION"
    val canSave = !saving && !state.blocksCompletion && (!isFlow || (editTitle.isNotBlank() && editTag.isNotBlank()))
    fun close() {
        if (saving) return
        if (state.blocksCompletion || state.draft.isNotBlank()) unfinished = true
        else if (editTitle != title || editTag != tagName) confirmClose = true
        else holder.quiesce(onClose)
    }
    fun save() {
        if (!canSave) return
        if (state.draft.isNotBlank()) { unfinished = true; return }
        saving = true
        holder.quiesce {
            scope.launch {
                try {
                    onSave(ownerType, ownerId, editTitle, editTag)
                    onClose()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    holder.resumeAfterPreCommitFailure()
                    error = true
                } finally { saving = false }
            }
        }
    }
    // A failed draft flush must leave Save available for another attempt.
    LaunchedEffect(state.hasError) { if (state.hasError) saving = false }
    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)) {
        BackHandler { close() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(if (isFlow) R.string.flow_card_edit_flow else R.string.pulse_card_edit_entry)) },
                    navigationIcon = { IconButton(onClick = { close() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back))
                    } },
                    actions = { TextButton(onClick = { save() }, enabled = canSave) {
                        Text(stringResource(R.string.common_save))
                    } }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                TabRow(selectedTabIndex = page) {
                    Tab(selected = page == 0, onClick = { if (!state.blocksPager) page = 0 },
                        text = { Text(stringResource(R.string.story_edit_details)) })
                    Tab(selected = page == 1, onClick = { page = 1 },
                        text = { Text(stringResource(R.string.chronicle_title)) })
                }
                if (error) Text(stringResource(R.string.story_edit_save_error), color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp))
                if (page == 0) Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it.take(60) },
                        label = { Text(stringResource(R.string.pulse_edit_title_label)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                    OutlinedTextField(value = editTag, onValueChange = { editTag = it },
                        label = { Text(stringResource(R.string.pulse_edit_journey_field_label)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                    OutlinedButton(onClick = { page = 1 }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.story_edit_chronicle))
                    }
                    Text(stringResource(R.string.story_edit_chronicle_hint), style = MaterialTheme.typography.bodyMedium)
                    extraDetails()
                } else {
                    Text(stringResource(R.string.story_edit_chronicle_autosave), style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                    ChroniclePage(holder, Modifier.weight(1f))
                }
            }
        }
        if (confirmClose) AlertDialog(onDismissRequest = { confirmClose = false },
            title = { Text(stringResource(R.string.story_edit_discard_title)) },
            text = { Text(stringResource(R.string.story_edit_discard_body)) },
            confirmButton = { TextButton(onClick = { holder.quiesce(onClose) }) { Text(stringResource(R.string.chronicle_discard)) } },
            dismissButton = { TextButton(onClick = { confirmClose = false }) { Text(stringResource(R.string.common_cancel)) } })
        if (unfinished) AlertDialog(onDismissRequest = { unfinished = false },
            title = { Text(stringResource(if (state.blocksCompletion) R.string.chronicle_finish_edit else R.string.chronicle_unfinished)) },
            confirmButton = { TextButton(onClick = { unfinished = false; page = 1 }) { Text(stringResource(R.string.story_edit_chronicle)) } },
            dismissButton = { TextButton(onClick = { unfinished = false }) { Text(stringResource(R.string.common_cancel)) } })
    }
}
