package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import com.kingkharnivore.skillz.ui.screen.shell.ux.ScyraParchmentSheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureEconomy
import com.kingkharnivore.skillz.utils.shell.CreatureStatus
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneId
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.ui.screen.shell.inventory.ShellMetricPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeyondBlueEncounterSheet(
    pearlBalance: Int,
    initialZone: TheBlueZoneId,
    activeAnimalInstances: List<UserShellFindInstanceEntity>,
    initialTargetSpeciesId: String? = null,
    onDismiss: () -> Unit,
    onEncounter: (String, List<String>) -> Unit
) {
    data class TradeStack(
        val key: String,
        val findId: String,
        val level: Int,
        val instances: List<UserShellFindInstanceEntity>,
        val perMinutes: Int
    )

    var confirmTargetId by remember(initialTargetSpeciesId) { mutableStateOf(initialTargetSpeciesId) }
    var selectedZone by remember(initialZone) { mutableStateOf(initialZone) }
    var selectedCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var tradeExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val contentListState = rememberLazyListState()
    val confirmTarget = confirmTargetId?.let { CreatureCatalog.get(it) }

    val tradeStacks = remember(activeAnimalInstances) {
        activeAnimalInstances
            .filter { it.creatureStatus == CreatureStatus.ACTIVE }
            .groupBy { "${it.findId}:${it.animalLevel.coerceAtLeast(1)}" }
            .map { (key, group) ->
                val first = group.first()
                val level = first.animalLevel.coerceAtLeast(1)
                TradeStack(
                    key = key,
                    findId = first.findId,
                    level = level,
                    instances = group.sortedBy { it.acquiredAt },
                    perMinutes = CreatureEconomy.beyondBlueTradeContributionMinutes(first.findId, level)
                )
            }
            .sortedBy { it.findId + ":" + it.level }
    }

    val selectedInstanceIds = remember(selectedCounts, tradeStacks) {
        buildList {
            tradeStacks.forEach { stack ->
                val selected = (selectedCounts[stack.key] ?: 0).coerceIn(0, stack.instances.size)
                stack.instances.take(selected).forEach { add(it.instanceId) }
            }
        }
    }
    val selectedMinutes = tradeStacks.sumOf { (selectedCounts[it.key] ?: 0) * it.perMinutes }
    val quote = confirmTarget?.let {
        CreatureEconomy.quoteBeyondBluePayment(it.creatureId, selectedMinutes, pearlBalance)
    }
    var showConfirm by remember(confirmTargetId, selectedCounts, quote) { mutableStateOf(false) }
    val navigateBackToCreatureList = {
        showConfirm = false
        confirmTargetId = null
        selectedCounts = emptyMap()
        tradeExpanded = false
    }
    val targetName = confirmTarget?.let { target ->
        target.titleRes.takeIf { it != 0 }?.let { stringResource(it) }
            ?: stringResource(R.string.badge_creature_fallback)
    }

    BackHandler(enabled = confirmTargetId != null) {
        navigateBackToCreatureList()
    }

    LaunchedEffect(confirmTargetId, selectedZone) {
        contentListState.scrollToItem(0)
    }

    ScyraParchmentSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            state = contentListState,
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "encounter_header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.beyond_blue_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.beyond_blue_zone_encounters, zoneTitle(selectedZone)))
                    Text(zoneSubtitle(selectedZone))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        TheBlueZoneId.entries.forEach { zone ->
                            FilterChip(selected = selectedZone == zone, onClick = { selectedZone = zone }, label = { Text(zoneRailLabel(zone)) })
                        }
                    }
                }
            }

            if (confirmTarget == null) {
                val selectedCreatureZone = selectedZone.toCreatureZone()
                items(
                    items = CreatureCatalog.beyondBlue.filter { it.zone == selectedCreatureZone },
                    key = { target -> target.creatureId }
                ) { target ->
                        val targetName = target.titleRes.takeIf { it != 0 }?.let { stringResource(it) }
                            ?: stringResource(R.string.badge_creature_fallback)
                        val requirement = target.requirementMinutes ?: 0
                        val price = CreatureEconomy.pearlPriceForRequirement(requirement)
                        val canAfford = pearlBalance >= price
                        ElevatedCard(onClick = {
                            confirmTargetId = target.creatureId
                            selectedCounts = emptyMap()
                            tradeExpanded = false
                        }, colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ShellObjectIcon(target.staticIconKey, Modifier.size(46.dp))
                                Text(targetName, fontWeight = FontWeight.Bold)
                                Text(zoneTitle(theBlueZoneFor(target.zone)), style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ShellMetricPill(Icons.Outlined.Diamond, stringResource(R.string.beyond_blue_or_pearls, price))
                                    ShellMetricPill(Icons.Outlined.Route, formatMinutesCompact(requirement))
                                    ShellMetricPill(Icons.Outlined.WaterDrop, if (canAfford) stringResource(R.string.beyond_blue_ready_to_buy) else stringResource(R.string.beyond_blue_need_more_pearls, (price - pearlBalance).coerceAtLeast(0)))
                                }
                            }
                        }
                }
            } else {
                val target = confirmTarget
                val name = checkNotNull(targetName)
                val paymentQuote = checkNotNull(quote)
                val requirement = target.requirementMinutes ?: 0
                val pearlOnlyPrice = CreatureEconomy.pearlPriceForRequirement(requirement)
                val progress = if (requirement == 0) 1f else (selectedMinutes.toFloat() / requirement.toFloat()).coerceIn(0f, 1f)

                item(key = "target_details") {
                    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShellObjectIcon(target.staticIconKey, Modifier.size(56.dp))
                            Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(zoneTitle(theBlueZoneFor(target.zone)), color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.beyond_blue_life_waiting_depth))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ShellMetricPill(Icons.Outlined.Route, formatMinutesCompact(requirement))
                                ShellMetricPill(Icons.Outlined.Diamond, stringResource(R.string.beyond_blue_or_pearls, pearlOnlyPrice))
                            }
                        }
                    }
                }

                item(key = "pearl_purchase") {
                    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.beyond_blue_buy_with_pearls), fontWeight = FontWeight.SemiBold)
                            ShellMetricPill(Icons.Outlined.Diamond, stringResource(R.string.beyond_blue_use_pearls_only_amount, pearlOnlyPrice))
                            Text(stringResource(R.string.beyond_blue_balance_pearls, pearlBalance))
                            Text(stringResource(R.string.beyond_blue_confirm_no_creatures_leave))
                            if (pearlBalance < pearlOnlyPrice) {
                                Text(stringResource(R.string.beyond_blue_need_more_pearls, pearlOnlyPrice - pearlBalance), color = MaterialTheme.colorScheme.error)
                                Text(stringResource(R.string.beyond_blue_trade_or_return_after_flow), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                            }
                            Button(
                                onClick = { showConfirm = true },
                                enabled = if (selectedInstanceIds.isEmpty()) pearlBalance >= pearlOnlyPrice else paymentQuote.canEncounter,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(if (selectedInstanceIds.isEmpty()) R.string.beyond_blue_buy_with_pearls else R.string.beyond_blue_trade_and_buy))
                            }
                        }
                    }
                }

                if (tradeStacks.isNotEmpty()) {
                    item(key = "trade_toggle") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.beyond_blue_optional), fontWeight = FontWeight.SemiBold)
                            OutlinedButton(onClick = { tradeExpanded = !tradeExpanded }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.beyond_blue_trade_reduce_optional))
                            }
                        }
                    }
                }

                if (tradeExpanded || selectedInstanceIds.isNotEmpty()) {
                    item(key = "adjusted_cost") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.beyond_blue_adjusted_cost), fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.beyond_blue_life_selected, formatMinutesCompact(paymentQuote.selectedCreatureMinutes)))
                                    Text(stringResource(R.string.beyond_blue_pearls_used, paymentQuote.pearlCostForRemaining))
                                    if (paymentQuote.pearlReturnForOverpay > 0) Text(stringResource(R.string.beyond_blue_payment_returned, paymentQuote.pearlReturnForOverpay))
                                    if (selectedInstanceIds.isNotEmpty()) {
                                        Button(onClick = { showConfirm = true }, enabled = paymentQuote.canEncounter, modifier = Modifier.fillMaxWidth()) {
                                            Text(stringResource(R.string.beyond_blue_trade_and_buy))
                                        }
                                    }
                                }
                            }
                            if (!paymentQuote.canEncounter) {
                                Text(stringResource(R.string.beyond_blue_need_more_pearls, (paymentQuote.pearlCostForRemaining - pearlBalance).coerceAtLeast(0)), color = MaterialTheme.colorScheme.error)
                                Text(stringResource(R.string.beyond_blue_trade_or_return_after_flow), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (tradeExpanded && tradeStacks.isNotEmpty()) {
                    item(key = "trade_summary") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.beyond_blue_trade_from_blue), fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.beyond_blue_calling_life_in), fontWeight = FontWeight.SemiBold)
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            Text(stringResource(R.string.beyond_blue_contribution_value, formatMinutesCompact(selectedMinutes), formatMinutesCompact(requirement)))
                        }
                    }

                    items(items = tradeStacks, key = { stack -> stack.key }) { stack ->
                        val selected = selectedCounts[stack.key] ?: 0
                        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ShellObjectIcon(CreatureCatalog.get(stack.findId)?.staticIconKey ?: "animal", Modifier.size(34.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(findName(stack.findId), fontWeight = FontWeight.Bold)
                                        Text(stringResource(R.string.shell_creature_level_short, stack.level))
                                    }
                                    ShellMetricPill(Icons.Outlined.Inventory2, stringResource(R.string.beyond_blue_owned_count, stack.instances.size))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    ShellMetricPill(Icons.Outlined.Route, stringResource(R.string.beyond_blue_each_value, formatMinutesCompact(stack.perMinutes)))
                                    ShellMetricPill(Icons.Outlined.Route, stringResource(R.string.beyond_blue_contributes_value, formatMinutesCompact(selected * stack.perMinutes)))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FilledTonalIconButton(
                                        onClick = { selectedCounts = selectedCounts + (stack.key to (selected - 1).coerceAtLeast(0)) },
                                        enabled = selected > 0,
                                        modifier = Modifier.size(48.dp),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                    ) {
                                        Icon(Icons.Default.Remove, stringResource(R.string.beyond_blue_remove_one, findName(stack.findId)), Modifier.size(20.dp))
                                    }
                                    Text(selected.toString(), Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    FilledTonalIconButton(
                                        onClick = { selectedCounts = selectedCounts + (stack.key to (selected + 1).coerceAtMost(stack.instances.size)) },
                                        enabled = selected < stack.instances.size,
                                        modifier = Modifier.size(48.dp),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                    ) {
                                        Icon(Icons.Default.Add, stringResource(R.string.beyond_blue_add_one, findName(stack.findId)), Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (tradeExpanded || selectedInstanceIds.isNotEmpty()) {
                    item(key = "trade_consequences") {
                        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (selectedInstanceIds.isEmpty()) {
                                    Text(stringResource(R.string.beyond_blue_confirm_no_creatures_leave))
                                    Text(stringResource(R.string.beyond_blue_pearls_call_life_in))
                                } else {
                                    Text(stringResource(R.string.beyond_blue_selected_leave))
                                    Text(stringResource(R.string.beyond_blue_lifetime_remains))
                                    if (paymentQuote.pearlCostForRemaining > 0) Text(stringResource(R.string.beyond_blue_pearls_cover_rest))
                                    if (paymentQuote.pearlReturnForOverpay > 0) Text(stringResource(R.string.beyond_blue_extra_value_returns))
                                }
                            }
                        }
                    }
                }

                item(key = "back_action") {
                    OutlinedButton(onClick = navigateBackToCreatureList, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.beyond_blue_back))
                    }
                }
            }
            item(key = "bottom_space") { Spacer(Modifier.height(10.dp)) }
        }

        val dialogTarget = confirmTarget
        val dialogQuote = quote
        if (showConfirm && dialogTarget != null && dialogQuote != null && targetName != null) {
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                containerColor = MaterialTheme.colorScheme.surface,
                confirmButton = {
                    Button(onClick = { onEncounter(dialogTarget.creatureId, selectedInstanceIds) }) {
                        Text(stringResource(if (selectedInstanceIds.isEmpty()) R.string.beyond_blue_buy else R.string.beyond_blue_trade_and_buy))
                    }
                },
                dismissButton = { OutlinedButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.beyond_blue_cancel)) } },
                title = { Text(stringResource(if (selectedInstanceIds.isEmpty()) R.string.beyond_blue_buy_with_pearls_title else R.string.beyond_blue_trade_and_buy_title, targetName)) },
                text = {
                    BackHandler { navigateBackToCreatureList() }
                    Column {
                        if (selectedInstanceIds.isEmpty()) {
                            Text(stringResource(R.string.beyond_blue_pearls_will_be_used, dialogQuote.pearlCostForRemaining))
                            Text(stringResource(R.string.beyond_blue_confirm_no_creatures_leave))
                        } else {
                            Text(stringResource(R.string.beyond_blue_selected_leave))
                            if (dialogQuote.pearlCostForRemaining > 0) Text(stringResource(R.string.beyond_blue_confirm_pearls_cover_remaining, dialogQuote.pearlCostForRemaining))
                            if (dialogQuote.pearlReturnForOverpay > 0) Text(stringResource(R.string.beyond_blue_confirm_returned, dialogQuote.pearlReturnForOverpay))
                            Text(stringResource(R.string.beyond_blue_lifetime_remains))
                        }
                        Text(stringResource(R.string.beyond_blue_confirm_enters_zone, targetName, zoneTitle(theBlueZoneFor(dialogTarget.zone))))
                    }
                }
            )
        }
    }
}
