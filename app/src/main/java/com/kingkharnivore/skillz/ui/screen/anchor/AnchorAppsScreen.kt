package com.kingkharnivore.skillz.ui.screen.anchor

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.viewmodel.anchor.AnchorAppsAction
import com.kingkharnivore.skillz.viewmodel.anchor.AnchorAppsViewModel
import com.kingkharnivore.skillz.viewmodel.anchor.AnchorableAppUiModel
import com.kingkharnivore.skillz.viewmodel.anchor.AnchoredAppUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorAppsScreen(
    viewModel: AnchorAppsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anchor Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Choose which apps Scyra should watch during active Flows. Essential apps stay available.",
                style = MaterialTheme.typography.bodyMedium
            )
            AnchorInfoCard("You are always in control. Anchor only watches the apps you choose, and you can pause it anytime.")
            AnchorInfoCard("Phone, emergency, alarms, Settings, and system apps are never anchored.")

            if (!state.usageAccessGranted) {
                AnchorInfoCard("Usage Access is needed to build this list. Scyra does not read your screen, messages, photos, keystrokes, app content, or browsing history.")
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
                    Text("Enable Usage Access")
                }
            }

            SectionTitle("Anchored Apps", "These are the apps Scyra will watch for during active Flows.")
            if (state.anchoredApps.isEmpty()) {
                Text("No apps anchored yet. Add apps from your recently used list below.")
            } else {
                state.anchoredApps.forEach { app ->
                    AnchoredAppRow(app) { viewModel.onAction(AnchorAppsAction.RemoveApp(app.packageName)) }
                }
            }

            SectionTitle(
                "Recently used",
                if (state.expandedTo30Days) "Last 30 days · Top 100 apps" else "Last 14 days · Top 40 apps"
            )
            state.recentlyUsedApps.forEach { app ->
                AnchorableAppRow(app) { viewModel.onAction(AnchorAppsAction.AnchorApp(app.packageName)) }
            }
            if (!state.expandedTo30Days) {
                OutlinedButton(onClick = { viewModel.onAction(AnchorAppsAction.ExpandTo30Days) }) {
                    Text("Show more from last 30 days")
                }
            }
            TextButton(onClick = { viewModel.onAction(AnchorAppsAction.RefreshRecentlyUsedApps) }) {
                Text("Refresh Recently Used Apps")
            }
            Text("Don’t see an app? Open it once, return to Scyra, then refresh this list.", style = MaterialTheme.typography.bodySmall)

            SectionTitle("Suggested distractions", "Common apps people choose to anchor.")
            state.suggestedApps.forEach { app ->
                AnchorableAppRow(app) { viewModel.onAction(AnchorAppsAction.AnchorApp(app.packageName)) }
            }
        }
    }
}

@Composable
private fun AnchorInfoCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) { Text(text, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@Composable
private fun AnchoredAppRow(app: AnchoredAppUiModel, onRemove: () -> Unit) {
    AppRow(title = app.displayName, subtitle = "Anchored during Flows", action = "Remove", enabled = true, onAction = onRemove)
}

@Composable
private fun AnchorableAppRow(app: AnchorableAppUiModel, onAnchor: () -> Unit) {
    AppRow(
        title = app.displayName,
        subtitle = app.label,
        action = if (app.available) "Anchor" else "Open once to add",
        enabled = app.available && !app.alreadyAnchored,
        onAction = onAnchor
    )
}

@Composable
private fun AppRow(title: String, subtitle: String, action: String, enabled: Boolean, onAction: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(enabled = enabled, onClick = onAction) { Text(action) }
        }
    }
}
