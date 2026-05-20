package com.kingkharnivore.skillz.ui.screen.shell.beyondblue

import com.kingkharnivore.skillz.ui.screen.shell.*
private fun BeyondBlueEncounterSheet(
    pearlBalance: Int,
    initialZone: TheBlueZoneId,
    activeAnimalInstances: List<UserShellFindInstanceEntity>,
    onDismiss: () -> Unit,
    onEncounter: (String, List<String>) -> Unit
) {
    var confirmTargetId by remember { mutableStateOf<String?>(null) }
    var selectedZone by remember(initialZone) { mutableStateOf(initialZone) }
    var selectedInstanceIds by remember { mutableStateOf(setOf<String>()) }
    val confirmTarget = confirmTargetId?.let { CreatureCatalog.get(it) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.beyond_blue_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.beyond_blue_intro))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TheBlueZoneId.entries.forEach { zone ->
                    val zoneA11y = zoneTitle(zone)
                    FilterChip(
                        selected = selectedZone == zone,
                        onClick = { selectedZone = zone },
                        label = { Text(zoneRailLabel(zone)) },
                        modifier = Modifier.semantics { contentDescription = zoneA11y }
                    )
                }
            }
            if (confirmTarget == null) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val selectedCreatureZone = selectedZone.toCreatureZone()
                    items(CreatureCatalog.beyondBlue.filter { it.zone == selectedCreatureZone }, key = { it.creatureId }) { target ->
                        val price = CreatureEconomy.pearlPriceForRequirement(target.requirementMinutes ?: 0)
                        val canAfford = pearlBalance >= price
                        ElevatedCard(
                            onClick = { confirmTargetId = target.creatureId },
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            ListItem(
                                leadingContent = { ShellObjectIcon(target.staticIconKey, Modifier.size(36.dp)) },
                                headlineContent = { Text(target.displayName) },
                                supportingContent = {
                                    Text("${target.zone.displayName}\n${stringResource(R.string.beyond_blue_requires_value, formatMinutesCompact(target.requirementMinutes ?: 0))}\n${stringResource(R.string.beyond_blue_or_pearls, price)}${if (canAfford) "" else "\n${stringResource(R.string.beyond_blue_need_more_pearls, (price - pearlBalance).coerceAtLeast(0))}"}")
                                }
                            )
                        }
                    }
                }
            } else {
                val target = confirmTarget
                val requirement = target.requirementMinutes ?: 0
                val pearlOnlyPrice = CreatureEconomy.pearlPriceForRequirement(requirement)
                val selectedInstances = activeAnimalInstances.filter { it.instanceId in selectedInstanceIds }
                val selectedMinutes = selectedInstances.sumOf { CreatureEconomy.beyondBlueTradeContributionMinutes(it.findId, it.animalLevel) }
                val quote = CreatureEconomy.quoteBeyondBluePayment(target.creatureId, selectedMinutes, pearlBalance)
                Text("${target.displayName} · ${target.zone.displayName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.beyond_blue_requires_value, formatMinutesCompact(requirement)) + " · " + stringResource(R.string.beyond_blue_or_pearls, pearlOnlyPrice))
                if (selectedInstanceIds.isNotEmpty()) {
                    Text(stringResource(R.string.beyond_blue_selected_leave))
                    Text(stringResource(R.string.beyond_blue_lifetime_remains))
                }
                if (activeAnimalInstances.isNotEmpty()) {
                    Text(stringResource(R.string.beyond_blue_available_trade), fontWeight = FontWeight.SemiBold)
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(activeAnimalInstances, key = { it.instanceId }) { item ->
                            val checked = item.instanceId in selectedInstanceIds
                            ElevatedCard(onClick = { selectedInstanceIds = if (checked) selectedInstanceIds - item.instanceId else selectedInstanceIds + item.instanceId }) {
                                ListItem(
                                    leadingContent = { ShellObjectIcon(CreatureCatalog.get(item.findId)?.staticIconKey ?: "animal", Modifier.size(32.dp)) },
                                    headlineContent = { Text(findName(item.findId)) },
                                    supportingContent = { Text("Level ${item.animalLevel} · ${zoneTitle(theBlueZoneFor(CreatureCatalog.get(item.findId)?.zone ?: CreatureZone.SUNLIT_REEF))}\n${formatMinutesCompact(CreatureEconomy.beyondBlueTradeContributionMinutes(item.findId, item.animalLevel))} creature value") },
                                    trailingContent = { Text(if (checked) stringResource(R.string.beyond_blue_selected) else stringResource(R.string.beyond_blue_tap_to_select)) }
                                )
                            }
                        }
                    }
                } else {
                    Text(stringResource(R.string.beyond_blue_no_active_to_trade))
                    Text(stringResource(R.string.beyond_blue_still_with_pearls))
                }
                Text(stringResource(R.string.beyond_blue_payment_summary), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.beyond_blue_payment_creatures, formatMinutesCompact(quote.selectedCreatureMinutes)))
                Text(stringResource(R.string.beyond_blue_payment_pearls, quote.pearlCostForRemaining))
                Text(stringResource(R.string.beyond_blue_payment_change, quote.pearlReturnForOverpay))
                if (!quote.canEncounter) {
                    Text(stringResource(R.string.beyond_blue_need_more_pearls, (quote.pearlCostForRemaining - pearlBalance).coerceAtLeast(0)), color = MaterialTheme.colorScheme.error)
                    Text(stringResource(R.string.beyond_blue_trade_to_reduce), color = MaterialTheme.colorScheme.error)
                }
                var showConfirm by remember(target.creatureId, selectedInstanceIds, quote) { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { confirmTargetId = null }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.beyond_blue_back)) }
                    Button(
                        onClick = { showConfirm = true },
                        enabled = quote.canEncounter,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.beyond_blue_encounter_cta)) }
                }
                if (showConfirm) {
                    AlertDialog(
                        onDismissRequest = { showConfirm = false },
                        confirmButton = { Button(onClick = { onEncounter(target.creatureId, selectedInstanceIds.toList()) }) { Text(stringResource(R.string.beyond_blue_encounter_cta)) } },
                        dismissButton = { OutlinedButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.beyond_blue_cancel)) } },
                        title = { Text(stringResource(R.string.beyond_blue_confirm_title, target.displayName)) },
                        text = {
                            Column {
                                Text(if (selectedInstanceIds.isEmpty()) stringResource(R.string.beyond_blue_confirm_no_creatures_leave) else stringResource(R.string.beyond_blue_selected_leave))
                                if (quote.pearlCostForRemaining > 0) Text(stringResource(R.string.beyond_blue_confirm_pearls_cover_remaining, quote.pearlCostForRemaining))
                                Text(stringResource(R.string.beyond_blue_confirm_enters_zone, target.displayName, zoneTitle(theBlueZoneFor(target.zone))))
                                if (selectedInstanceIds.isNotEmpty()) Text(stringResource(R.string.beyond_blue_lifetime_remains))
                                if (quote.pearlReturnForOverpay > 0) Text(stringResource(R.string.beyond_blue_confirm_returned, quote.pearlReturnForOverpay))
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TheBlueDepthRail(
    zones: List<TheBlueZoneId>,
    activeZone: TheBlueZoneId,
    onZoneClick: (TheBlueZoneId) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val railDescription = stringResource(R.string.the_blue_depth_rail_a11y)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = scheme.surface.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.18f)),
        modifier = modifier
            .fillMaxHeight(0.48f)
            .width(58.dp)
            .semantics { contentDescription = railDescription }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            zones.forEach { zone ->
                val active = zone == activeZone
                val title = zoneTitle(zone)
                val goToDescription = stringResource(R.string.the_blue_depth_rail_go_to_zone_a11y, title)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button) { onZoneClick(zone) }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .semantics {
                            contentDescription = goToDescription
                            role = Role.Button
                        }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (active) scheme.secondary else scheme.primary.copy(alpha = 0.24f),
                        modifier = Modifier.size(if (active) 12.dp else 8.dp),
                        content = {}
                    )
                    Text(
                        text = zoneRailLabel(zone),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) scheme.secondary else scheme.onSurface.copy(alpha = 0.58f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 36.dp, max = 52.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun zoneTitle(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_title)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_title)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_title)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_title)
}

@Composable
private fun zoneRailLabel(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_rail)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_rail)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_rail)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_rail)
}

@Composable
private fun zoneSubtitle(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_subtitle)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_subtitle)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_subtitle)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_subtitle)
}

@Composable
private fun findName(findId: String): String = ShellContentCatalog.find(findId)?.let { stringResource(it.titleRes) } ?: stringResource(R.string.reward_card_shell_recorded_title)

@Composable
private fun formName(findId: String, stageId: String?): String =
    ShellContentCatalog.upgradesFor(findId).firstOrNull { it.upgradeStageId == stageId }?.let { stringResource(it.titleRes) }
        ?: stringResource(R.string.shell_form_base)

@Composable
private fun theBlueSourceReason(findId: String): String = when (findId) {
    ShellContentCatalog.FOCUS_MINNOW -> stringResource(R.string.the_blue_source_minnow)
    ShellContentCatalog.FOCUS_SEAHORSE -> stringResource(R.string.the_blue_source_seahorse)
    ShellContentCatalog.FOCUS_MANTA -> stringResource(R.string.the_blue_source_manta)
    ShellContentCatalog.FOCUS_WHALE -> stringResource(R.string.the_blue_source_whale)
    ShellContentCatalog.FOCUS_OCTOPUS -> stringResource(R.string.the_blue_source_octopus)
    else -> ShellContentCatalog.find(findId)?.let { stringResource(it.descriptionRes) } ?: stringResource(R.string.reward_card_shell_recorded_body)
}

@Composable
private fun theBlueEncounteredReason(findId: String): String = when (findId) {
    ShellContentCatalog.FOCUS_MINNOW -> stringResource(R.string.the_blue_encountered_minnow)
    ShellContentCatalog.FOCUS_SEAHORSE -> stringResource(R.string.the_blue_encountered_seahorse)
    ShellContentCatalog.FOCUS_MANTA -> stringResource(R.string.the_blue_encountered_manta)
    ShellContentCatalog.FOCUS_WHALE -> stringResource(R.string.the_blue_encountered_whale)
    ShellContentCatalog.FOCUS_OCTOPUS -> stringResource(R.string.the_blue_source_octopus)
    else -> theBlueSourceReason(findId)
}

private fun firstOpenFocusSlotFor(findId: String, uiState: ShellUiState): String? {
    val definition = ShellContentCatalog.find(findId) ?: return null
    val occupied = uiState.focusPlacements.map { it.slotId }.toSet()
    return ShellContentCatalog.focusSlots.firstOrNull { slot ->
        slot.slotId !in occupied && ShellContentCatalog.isCompatibleWithSlot(slot, definition)
    }?.slotId
}

private fun firstRestingInstanceId(findId: String, uiState: ShellUiState): String? {
    val displayed = uiState.focusPlacements.map { it.instanceId }.toSet()
    return uiState.finds.firstOrNull { item ->
        item.findId == findId && item.instanceId !in displayed && canDisplayInstance(item, ShellContentCatalog.find(item.findId))
    }?.instanceId
}

@Composable
