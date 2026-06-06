package com.kingkharnivore.skillz.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.localization.AppLanguage
import androidx.health.connect.client.PermissionController
import com.kingkharnivore.skillz.model.state.FlowListUiState
import com.kingkharnivore.skillz.ui.health.DisableHealthPendingFlowsDialog
import com.kingkharnivore.skillz.ui.health.HealthConnectSettingsCard
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kingkharnivore.skillz.viewmodel.health.HealthSettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private fun logHealthDebug(message: String) {
    if (BuildConfig.DEBUG) {
        Log.d("HealthSettings", message)
    }
}

@Composable
fun HelpScreen(
    uiState: FlowListUiState,
    selectedLanguageTag: String?,
    healthViewModel: HealthSettingsViewModel,
    onToggleShowScoreUi: (Boolean) -> Unit,
    onToggleCalmMode: (Boolean) -> Unit,
    onSetAppLanguage: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = helpPages()

    val titleText = stringResource(R.string.help_screen_title)
    val keepScoreTitle = stringResource(R.string.help_pref_keep_score_title)
    val keepScoreDescription = stringResource(R.string.help_pref_keep_score_description)
    val calmModeTitle = stringResource(R.string.help_pref_calm_mode_title)
    val calmModeDescription = stringResource(R.string.help_pref_calm_mode_description)

    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    val healthState by healthViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        logHealthDebug("Health permission result=$granted")
        healthViewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) { healthViewModel.refreshState() }
    LaunchedEffect(healthState) {
        logHealthDebug("Health card state=$healthState")
        logHealthDebug("packageName=${context.packageName}")
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                healthViewModel.refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = titleText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
            modifier = Modifier.semantics { heading() }
        )

        Spacer(Modifier.height(2.dp))

        PreferenceToggleRow(
            title = keepScoreTitle,
            description = keepScoreDescription,
            checked = uiState.showScoreUi,
            onCheckedChange = onToggleShowScoreUi
        )

        PreferenceToggleRow(
            title = calmModeTitle,
            description = calmModeDescription,
            checked = uiState.calmMode,
            onCheckedChange = onToggleCalmMode
        )

        LanguagePreferenceRow(
            selectedLanguageTag = selectedLanguageTag,
            onClick = { showLanguageDialog = true }
        )

        HealthConnectSettingsCard(
            state = healthState,
            onConnectHealth = {
                logHealthDebug(
                    "Connect Health clicked availability=${healthState.healthConnectAvailability} permission=${healthViewModel.readStepsPermission}"
                )
                try {
                    healthPermissionLauncher.launch(setOf(healthViewModel.readStepsPermission))
                } catch (t: Throwable) {
                    healthViewModel.onPermissionLaunchFailed(t)
                }
            },
            onToggleMovementBonus = { checked ->
                if (checked) {
                    healthViewModel.enableMovementBonusIfPermissionGranted()
                } else {
                    healthViewModel.requestDisableOrDisableNow()
                }
            },
            onInstallOrUpdateHealthConnect = {
                healthViewModel.openHealthConnectInstallOrUpdate(context)
            }
        )

        HelpConceptCarousel(
            pages = pages,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (healthState.showDisableWarning) {
        DisableHealthPendingFlowsDialog(
            onKeepHealthOn = healthViewModel::keepHealthOn,
            onDisableAnyway = healthViewModel::disableAnyway
        )
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            selectedLanguageTag = selectedLanguageTag,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { tag ->
                showLanguageDialog = false
                onSetAppLanguage(tag)
            }
        )
    }
}

@Composable
private fun LanguagePreferenceRow(
    selectedLanguageTag: String?,
    onClick: () -> Unit
) {
    val title = stringResource(R.string.help_language_title)
    val description = stringResource(R.string.help_language_description)
    val currentLanguage = AppLanguage.fromTag(selectedLanguageTag)
    val currentLabel = stringResource(currentLanguage.labelRes)

    val rowA11y = stringResource(
        R.string.help_language_row_a11y,
        title,
        description,
        currentLabel
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = rowA11y
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = currentLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguagePickerDialog(
    selectedLanguageTag: String?,
    onDismiss: () -> Unit,
    onLanguageSelected: (String?) -> Unit
) {
    val current = AppLanguage.fromTag(selectedLanguageTag)
    var pending by rememberSaveable { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.help_language_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pending = language }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = pending == language,
                            onClick = { pending = language }
                        )
                        Text(
                            text = stringResource(language.labelRes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onLanguageSelected(pending.tag) }
            ) {
                Text(stringResource(R.string.help_language_dialog_confirm))
            }
        }
    )
}

@Composable
private fun HelpConceptCarousel(
    pages: List<HelpPage>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val carouselA11y = stringResource(R.string.help_carousel_a11y)
    val counterText = stringResource(
        R.string.help_carousel_counter,
        pagerState.currentPage + 1,
        pages.size
    )
    val currentPageA11y = stringResource(
        R.string.help_carousel_page_a11y,
        pagerState.currentPage + 1,
        pages.size
    )

    Column(
        modifier = modifier.semantics {
            contentDescription = carouselA11y
            stateDescription = currentPageA11y
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 34.dp),
                pageSpacing = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val pageOffset =
                    ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        .absoluteValue
                        .coerceIn(0f, 1f)

                val scale = 1f - (pageOffset * 0.035f)
                val alpha = 1f - (pageOffset * 0.10f)

                HelpInfoCard(
                    page = pages[page],
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 42.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 2.dp,
                shadowElevation = 6.dp
            ) {
                Text(
                    text = counterText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        ElegantPagerIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            onPageSelected = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            }
        )
    }
}

@Composable
private fun ElegantPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val onText = stringResource(R.string.pref_toggle_on)
    val offText = stringResource(R.string.pref_toggle_off)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val selected = index == currentPage
                val width by animateDpAsState(
                    targetValue = if (selected) 18.dp else 6.dp,
                    label = "help_indicator_width"
                )
                val dotA11y = stringResource(R.string.help_carousel_select_page, index + 1)
                val stateText = if (selected) onText else offText

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onPageSelected(index) }
                        .semantics {
                            role = Role.Button
                            contentDescription = dotA11y
                            stateDescription = stateText
                        }
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                            }
                        )
                        .size(width = width, height = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun HelpCardKicker(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun HelpInfoCard(
    page: HelpPage,
    modifier: Modifier = Modifier
) {
    val tipA11y = stringResource(R.string.help_card_tip_a11y, page.tip)
    val cardA11y = stringResource(
        R.string.help_card_a11y,
        page.kicker,
        page.title,
        page.subtitle,
        page.body,
        page.tip
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 245.dp)
            .clearAndSetSemantics {
                contentDescription = cardA11y
            },
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HelpCardKicker(
                iconRes = page.iconRes,
                label = page.kicker
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )

                Text(
                    text = page.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }

            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
            )

            Spacer(Modifier.weight(1f))

            Surface(
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = tipA11y
                },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.60f)
            ) {
                Text(
                    text = page.tip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val onText = stringResource(R.string.pref_toggle_on)
    val offText = stringResource(R.string.pref_toggle_off)
    val stateText = if (checked) onText else offText
    val a11yText = stringResource(
        R.string.pref_toggle_a11y,
        title,
        description,
        stateText
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.semantics {
            contentDescription = a11yText
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.semantics {
                    stateDescription = stateText
                }
            )
        }
    }
}

private data class HelpPage(
    @DrawableRes val iconRes: Int,
    val kicker: String,
    val title: String,
    val subtitle: String,
    val body: String,
    val tip: String
)

@Composable
private fun helpPages(): List<HelpPage> = listOf(
    HelpPage(
        iconRes = R.drawable.scyra_turtle,
        kicker = stringResource(R.string.help_page_flow_kicker),
        title = stringResource(R.string.help_page_flow_title),
        subtitle = stringResource(R.string.help_page_flow_subtitle),
        body = stringResource(R.string.help_page_flow_body),
        tip = stringResource(R.string.help_page_flow_tip)
    ),
    HelpPage(
        iconRes = R.drawable.scyra_turtle,
        kicker = stringResource(R.string.help_page_soft_flow_kicker),
        title = stringResource(R.string.help_page_soft_flow_title),
        subtitle = stringResource(R.string.help_page_soft_flow_subtitle),
        body = stringResource(R.string.help_page_soft_flow_body),
        tip = stringResource(R.string.help_page_soft_flow_tip)
    ),
    HelpPage(
        iconRes = R.drawable.scyra_turtle,
        kicker = stringResource(R.string.help_page_pulse_kicker),
        title = stringResource(R.string.help_page_pulse_title),
        subtitle = stringResource(R.string.help_page_pulse_subtitle),
        body = stringResource(R.string.help_page_pulse_body),
        tip = stringResource(R.string.help_page_pulse_tip)
    ),
    HelpPage(
        iconRes = R.drawable.scyra_turtle,
        kicker = stringResource(R.string.help_page_arc_kicker),
        title = stringResource(R.string.help_page_arc_title),
        subtitle = stringResource(R.string.help_page_arc_subtitle),
        body = stringResource(R.string.help_page_arc_body),
        tip = stringResource(R.string.help_page_arc_tip)
    ),
    HelpPage(
        iconRes = R.drawable.scyra_turtle,
        kicker = stringResource(R.string.help_page_surge_kicker),
        title = stringResource(R.string.help_page_surge_title),
        subtitle = stringResource(R.string.help_page_surge_subtitle),
        body = stringResource(R.string.help_page_surge_body),
        tip = stringResource(R.string.help_page_surge_tip)
    )
)