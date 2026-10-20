package com.kingkharnivore.skillz.ui.screen.story

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.FlowMode
import kotlinx.coroutines.launch

/** Navigation returns false when an existing Flow prevents launching another one. */
@Composable
internal fun rememberObjectiveFlowLauncher(
    snackbar: SnackbarHostState,
    onLaunch: (String, FlowMode) -> Boolean
): (String, FlowMode) -> Unit {
    val latestLaunch by rememberUpdatedState(onLaunch)
    val scope = rememberCoroutineScope()
    val activeFlowMessage = stringResource(R.string.lookout_flow_already_active)
    return remember(snackbar, scope, activeFlowMessage) {
        { journey, mode ->
            if (!latestLaunch(journey, mode)) {
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(activeFlowMessage)
                }
            }
        }
    }
}
