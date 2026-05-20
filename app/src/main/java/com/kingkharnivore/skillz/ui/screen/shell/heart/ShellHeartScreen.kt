package com.kingkharnivore.skillz.ui.screen.shell.heart

import com.kingkharnivore.skillz.ui.screen.shell.*
private fun HeartRoomScreen(
    uiState: ShellUiState,
    onNavigate: (ShellDestination) -> Unit,
    onOpenPearlBasin: () -> Unit
) {
    var showHeartDetail by remember { mutableStateOf(false) }
    val chestHasIndicator = restingFinds(uiState).any { it.isNew } || uiState.stacks.any { it.isNew && isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) }
    val hasNewDiscovery = uiState.discoveries.any { it.isNew }
    val focusChanged = hasAffordableFocusPearlAction(uiState)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HeartShellBackground(
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val nodeWidth = if (maxWidth < 360.dp) 96.dp else 108.dp
                val nodeHeight = if (maxHeight < 440.dp) 76.dp else 84.dp

                RoomOrbitNode(
                    labelRes = R.string.shell_room_lookout_title,
                    icon = Icons.Outlined.Visibility,
                    dormant = true,
                    nodeWidth = nodeWidth,
                    nodeHeight = nodeHeight,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 4.dp),
                    onClick = { onNavigate(ShellDestination.LookoutPreview) }
                )

                RoomOrbitPair(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = maxHeight * 0.17f),
                    nodeWidth = nodeWidth,
                    left = {
                        RoomOrbitNode(
                            labelRes = R.string.shell_room_voyage_title,
                            icon = Icons.Outlined.Route,
                            dormant = true,
                            nodeWidth = nodeWidth,
                            nodeHeight = nodeHeight,
                            onClick = { onNavigate(ShellDestination.VoyagePreview) }
                        )
                    },
                    right = {
                        RoomOrbitNode(
                            labelRes = R.string.shell_room_idea_title,
                            icon = Icons.Outlined.PsychologyAlt,
                            dormant = true,
                            nodeWidth = nodeWidth,
                            nodeHeight = nodeHeight,
                            onClick = { onNavigate(ShellDestination.IdeaGrovePreview) }
                        )
                    }
                )

                HeartCenter(
                    uiState = uiState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = maxHeight * 0.36f),
                    onClick = { showHeartDetail = true },
                    onPearlClick = onOpenPearlBasin
                )

                RoomOrbitPair(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = maxHeight * 0.61f),
                    nodeWidth = nodeWidth,
                    left = {
                        RoomOrbitNode(
                            labelRes = R.string.shell_room_focus_title,
                            icon = Icons.Outlined.CenterFocusStrong,
                            dormant = false,
                            hasIndicator = focusChanged,
                            nodeWidth = nodeWidth,
                            nodeHeight = nodeHeight,
                            onClick = { onNavigate(ShellDestination.Focus) }
                        )
                    },
                    right = {
                        RoomOrbitNode(
                            labelRes = R.string.shell_room_stillwater_title,
                            icon = Icons.Outlined.WaterDrop,
                            dormant = false,
                            nodeWidth = nodeWidth,
                            nodeHeight = nodeHeight,
                            onClick = { onNavigate(ShellDestination.Stillwater) }
                        )
                    }
                )

                RoomOrbitNode(
                    labelRes = R.string.shell_room_the_blue_title,
                    icon = Icons.Outlined.FilterVintage,
                    dormant = false,
                    hasIndicator = buildTheBlueUiState(uiState.finds, uiState.focusPlacements).newAnimalCount > 0,
                    nodeWidth = nodeWidth,
                    nodeHeight = nodeHeight,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = maxHeight * 0.80f),
                    onClick = { onNavigate(ShellDestination.TheBluePreview) }
                )
            }

            ShellWhisperDock(
                uiState = uiState,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val hasEmptyNook = uiState.focusPlacements.size < ShellContentCatalog.focusSlots.size
                    when {
                        uiState.discoveries.any { it.isNew } -> onNavigate(ShellDestination.DiscoveryJournal)
                        hasAffordablePearlShape(uiState) -> onNavigate(ShellDestination.Focus)
                        hasRestingPlaceableFinds(uiState) -> onNavigate(ShellDestination.ShellChest)
                        hasEmptyNook -> onNavigate(ShellDestination.Focus)
                        else -> onNavigate(ShellDestination.Focus)
                    }
                }
            )

            HeartShortcutDock(
                chestHasIndicator = chestHasIndicator,
                journalHasIndicator = hasNewDiscovery,
                onChest = { onNavigate(ShellDestination.ShellChest) },
                onBadges = { onNavigate(ShellDestination.Badges) },
                onJournal = { onNavigate(ShellDestination.DiscoveryJournal) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showHeartDetail) {
        HeartDetailSheet(
            uiState = uiState,
            onDismiss = { showHeartDetail = false },
            onOpenPearlBasin = {
                showHeartDetail = false
                onOpenPearlBasin()
            },
            onOpenFocus = {
                showHeartDetail = false
                onNavigate(ShellDestination.Focus)
            },
            onOpenChest = {
                showHeartDetail = false
                onNavigate(ShellDestination.ShellChest)
            },
            onOpenJournal = {
                showHeartDetail = false
                onNavigate(ShellDestination.DiscoveryJournal)
            }
        )
    }
}

@Composable
private fun RoomOrbitPair(
    modifier: Modifier = Modifier,
    nodeWidth: Dp,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(0.92f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(nodeWidth)) {
            left()
        }

        Box(Modifier.width(nodeWidth)) {
            right()
        }
    }
}

@Composable
private fun RoomOrbitNode(
    labelRes: Int,
    icon: ImageVector,
    dormant: Boolean,
    hasIndicator: Boolean = false,
    nodeWidth: Dp,
    nodeHeight: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(labelRes)
    val nodeDescription = if (dormant) {
        stringResource(R.string.shell_room_preview_a11y, label)
    } else {
        stringResource(R.string.shell_room_active_a11y, label)
    }

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (dormant) {
                scheme.surface.copy(alpha = 0.82f)
            } else {
                scheme.surface
            }
        ),
        modifier = modifier
            .width(nodeWidth)
            .height(nodeHeight)
            .semantics {
                contentDescription = nodeDescription
                role = Role.Button
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            TurtleShellCardPattern(Modifier.matchParentSize())

            if (hasIndicator) {
                Surface(
                    shape = CircleShape,
                    color = shellIndicatorColor(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(10.dp),
                    content = {}
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (dormant) {
                        scheme.primary.copy(alpha = 0.64f)
                    } else {
                        scheme.primary
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dormant) {
                        scheme.onSurface.copy(alpha = 0.76f)
                    } else {
                        scheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun HeartCenter(
    uiState: ShellUiState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPearlClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val heartDescription = stringResource(R.string.shell_heart_center_a11y)
    val pearlBalanceDescription = stringResource(R.string.shell_pearl_basin_chip_a11y, uiState.pearlBalance)

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = scheme.surface
        ),
        modifier = modifier
            .width(214.dp)
            .semantics {
                contentDescription = heartDescription
                role = Role.Button
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = scheme.primary,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Spa,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.shell_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            AssistChip(
                onClick = onPearlClick,
                label = {
                    Text(
                        text = stringResource(R.string.shell_pearl_balance, uiState.pearlBalance),
                        textAlign = TextAlign.Center
                    )
                },
                leadingIcon = {
                    ShellPearlMiniIcon(Modifier.size(18.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = scheme.background,
                    labelColor = scheme.onBackground,
                    leadingIconContentColor = scheme.primary
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = scheme.secondary.copy(alpha = 0.55f)
                ),
                modifier = Modifier.semantics {
                    contentDescription = pearlBalanceDescription
                    role = Role.Button
                }
            )
        }
    }
}

@Composable
private fun ShellWhisperDock(
    uiState: ShellUiState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val displayedIds = displayedInstanceIds(uiState)
    val affordableUpgrade = uiState.finds.firstOrNull { item ->
        val def = ShellContentCatalog.find(item.findId) ?: return@firstOrNull false
        if (def.kind != ShellRewardKind.OBJECT) return@firstOrNull false
        val next = ShellContentCatalog.nextUpgrade(def.findId, item.currentUpgradeStageId) ?: return@firstOrNull false
        item.instanceId in displayedIds && next.pearlCost <= uiState.pearlBalance
    }
    val restingCount = restingFinds(uiState).count { item -> ShellContentCatalog.find(item.findId)?.placeable == true }
    val hasEmptyNook = uiState.focusPlacements.size < ShellContentCatalog.focusSlots.size

    val text = when {
        uiState.discoveries.any { it.isNew } -> stringResource(R.string.shell_whisper_new_discovery)
        affordableUpgrade != null -> stringResource(R.string.shell_whisper_upgrade_ready, ShellContentCatalog.find(affordableUpgrade.findId)?.let { stringResource(it.titleRes) } ?: stringResource(R.string.shell_empty_slot))
        restingCount > 0 -> stringResource(R.string.shell_whisper_chest_waiting, restingCount)
        hasEmptyNook -> stringResource(R.string.shell_whisper_empty_focus)
        else -> stringResource(R.string.shell_pulse_mystery)
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = scheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.35f)),
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoStories,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurface.copy(alpha = 0.82f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HeartShortcutDock(
    chestHasIndicator: Boolean,
    journalHasIndicator: Boolean,
    onChest: () -> Unit,
    onBadges: () -> Unit,
    onJournal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.30f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeartShortcut(
                icon = Icons.Outlined.Inventory2,
                labelRes = R.string.shell_chest_title,
                hasIndicator = chestHasIndicator,
                onClick = onChest
            )

            HeartShortcut(
                icon = Icons.Outlined.MilitaryTech,
                labelRes = R.string.shell_badges_title,
                hasIndicator = false,
                onClick = onBadges
            )

            HeartShortcut(
                icon = Icons.Outlined.AutoStories,
                labelRes = R.string.shell_journal_title,
                hasIndicator = journalHasIndicator,
                onClick = onJournal
            )
        }
    }
}

@Composable
private fun HeartShortcut(
    icon: ImageVector,
    labelRes: Int,
    hasIndicator: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(labelRes)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(22.dp)
            )
            if (hasIndicator) {
                Surface(
                    shape = CircleShape,
                    color = shellIndicatorColor(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp),
                    content = {}
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HeartDetailSheet(
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onOpenPearlBasin: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenChest: () -> Unit,
    onOpenJournal: () -> Unit
) {
    val totalFinds = uiState.finds.count { isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) } +
        uiState.stacks.filter { isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) }.sumOf { it.quantity }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.shell_heart_detail_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.shell_heart_pearls_gathered, uiState.pearlBalance),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = stringResource(R.string.shell_heart_finds_owned, totalFinds),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.shell_heart_discoveries_awakened, uiState.discoveries.size),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.shell_heart_objects_displayed_resting, uiState.focusPlacements.size, restingFinds(uiState).size),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onOpenPearlBasin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_pearl_basin_title))
                }

                OutlinedButton(
                    onClick = onOpenFocus,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_room_focus_title))
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onOpenChest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_chest_title))
                }

                OutlinedButton(
                    onClick = onOpenJournal,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_journal_title))
                }
            }
        }
    }
}

@Composable
private fun PearlBasinSheet(
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenChest: () -> Unit,
    onOpenObject: () -> Unit,
    onInviteObject: (String) -> Unit
) {
    var inviteConfirmation by remember { mutableStateOf<ShellFindDefinition?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShellPearlBasinIcon(Modifier.size(52.dp))

                Column {
                    Text(
                        text = stringResource(R.string.shell_pearl_basin_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.shell_pearl_balance, uiState.pearlBalance),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(stringResource(R.string.shell_pearl_basin_copy))

            val displayedIds = displayedInstanceIds(uiState)
            val upgradeSuggestions = uiState.finds.mapNotNull { item ->
                val def = ShellContentCatalog.find(item.findId) ?: return@mapNotNull null
                if (def.kind != ShellRewardKind.OBJECT) return@mapNotNull null
                val next = ShellContentCatalog.nextUpgrade(def.findId, item.currentUpgradeStageId) ?: return@mapNotNull null
                Triple(item, def, next)
            }.sortedBy { it.third.pearlCost }
            val displayedUpgradeSuggestions = upgradeSuggestions.filter { it.first.instanceId in displayedIds }
            val objectSuggestions = ShellContentCatalog.focusPearlObjects
                .sortedBy { it.pearlCost ?: Int.MAX_VALUE }
            val allCosts = displayedUpgradeSuggestions.map { it.third.pearlCost } + objectSuggestions.mapNotNull { it.pearlCost }
            val hasAvailable = allCosts.any { it <= uiState.pearlBalance }

            Text(
                text = stringResource(R.string.shell_available_now),
                fontWeight = FontWeight.SemiBold
            )
            if (hasAvailable) {
                displayedUpgradeSuggestions.filter { it.third.pearlCost <= uiState.pearlBalance }.take(2).forEach { (item, def, next) ->
                    SuggestionRow(
                        title = stringResource(R.string.shell_basin_brighten_suggestion, stringResource(def.titleRes)),
                        cost = next.pearlCost,
                        onClick = onOpenObject
                    )
                }
                objectSuggestions.filter { (it.pearlCost ?: 0) <= uiState.pearlBalance }.take(2).forEach { def ->
                    SuggestionRow(
                        title = stringResource(R.string.shell_basin_invite_suggestion, stringResource(def.titleRes)),
                        cost = def.pearlCost ?: 0,
                        onClick = { inviteConfirmation = def }
                    )
                }
            } else {
                Text(stringResource(R.string.shell_no_available_shapes))
            }

            Text(
                text = stringResource(R.string.shell_affordable_soon),
                fontWeight = FontWeight.SemiBold
            )
            (displayedUpgradeSuggestions.filter { it.third.pearlCost > uiState.pearlBalance }.map { (_, def, next) ->
                stringResource(R.string.shell_basin_soon_upgrade, stringResource(def.titleRes), stringResource(next.titleRes)) to next.pearlCost
            } + objectSuggestions.filter { (it.pearlCost ?: 0) > uiState.pearlBalance }.map { def ->
                stringResource(R.string.shell_basin_invite_suggestion, stringResource(def.titleRes)) to (def.pearlCost ?: 0)
            })
                .sortedBy { it.second }
                .take(3)
                .forEach { (title, cost) ->
                    SuggestionRow(
                        title = title,
                        cost = cost,
                        enabled = false,
                        supportingText = stringResource(R.string.shell_need_more_pearls, cost - uiState.pearlBalance),
                        onClick = {}
                    )
                }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onOpenFocus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_room_focus_title))
                }

                OutlinedButton(
                    onClick = onOpenChest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_chest_title))
                }
            }
        }
    }

    inviteConfirmation?.let { def ->
        InvitePearlObjectConfirmationSheet(
            definition = def,
            onDismiss = { inviteConfirmation = null },
            onConfirm = {
                inviteConfirmation = null
                onInviteObject(def.findId)
            }
        )
    }
}

@Composable
private fun InvitePearlObjectConfirmationSheet(
    definition: ShellFindDefinition,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title = stringResource(definition.titleRes)
    val cost = definition.pearlCost ?: 0

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.shell_invite_confirm_title, title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.shell_invite_confirm_body, title))
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(stringResource(R.string.shell_invite_confirm_cta, cost))
            }
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.shell_cancel))
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    title: String,
    cost: Int,
    enabled: Boolean = true,
    supportingText: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supportingText ?: stringResource(R.string.shell_pearl_cost, cost)) },
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }
    )
}

@Composable
private fun HeartShellBackground(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = scheme.primary.copy(alpha = 0.20f),
            topLeft = Offset(-w * 0.16f, h * 0.02f),
            size = Size(w * 1.32f, h * 0.96f)
        )

        drawOval(
            color = scheme.secondary.copy(alpha = 0.10f),
            topLeft = Offset(w * 0.08f, h * 0.09f),
            size = Size(w * 0.84f, h * 0.76f)
        )

        val centerSeam = Path().apply {
            moveTo(w * 0.50f, h * 0.07f)
            cubicTo(
                w * 0.46f,
                h * 0.26f,
                w * 0.54f,
                h * 0.48f,
                w * 0.50f,
                h * 0.86f
            )
        }

        drawPath(
            path = centerSeam,
            color = scheme.secondary.copy(alpha = 0.18f),
            style = Stroke(width = 4f)
        )

        listOf(0.20f, 0.35f, 0.50f, 0.66f, 0.80f).forEach { yFraction ->
            val y = h * yFraction

            val band = Path().apply {
                moveTo(w * 0.12f, y)
                cubicTo(
                    w * 0.30f,
                    y - h * 0.05f,
                    w * 0.43f,
                    y + h * 0.025f,
                    w * 0.50f,
                    y
                )
                cubicTo(
                    w * 0.57f,
                    y + h * 0.025f,
                    w * 0.70f,
                    y - h * 0.05f,
                    w * 0.88f,
                    y
                )
            }

            drawPath(
                path = band,
                color = scheme.secondary.copy(alpha = 0.10f),
                style = Stroke(width = 2.5f)
            )
        }

        drawOval(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(-w * 0.10f, h * 0.02f),
            size = Size(w * 1.20f, h * 0.96f),
            style = Stroke(width = w * 0.10f)
        )
    }
}

@Composable
private fun ShellPearlBasinIcon(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = scheme.secondary.copy(alpha = 0.90f),
            topLeft = Offset(w * 0.08f, h * 0.32f),
            size = Size(w * 0.84f, h * 0.50f)
        )

        drawOval(
            color = scheme.surface.copy(alpha = 0.86f),
            topLeft = Offset(w * 0.16f, h * 0.36f),
            size = Size(w * 0.68f, h * 0.34f)
        )

        repeat(4) { index ->
            val x = w * (0.30f + index * 0.13f)
            val groove = Path().apply {
                moveTo(x, h * 0.42f)
                cubicTo(
                    x - w * 0.04f,
                    h * 0.52f,
                    x - w * 0.02f,
                    h * 0.62f,
                    x,
                    h * 0.70f
                )
            }

            drawPath(
                path = groove,
                color = scheme.onSecondary.copy(alpha = 0.26f),
                style = Stroke(width = 2f)
            )
        }

        drawCircle(
            color = scheme.onPrimary,
            radius = w * 0.16f,
            center = Offset(w * 0.50f, h * 0.36f)
        )

        drawCircle(
            color = scheme.primary.copy(alpha = 0.40f),
            radius = w * 0.08f,
            center = Offset(w * 0.56f, h * 0.31f)
        )
    }
}

@Composable
private fun shellIndicatorColor(): Color {
    val scheme = MaterialTheme.colorScheme
    val secondaryContrast = contrastRatio(scheme.secondary, scheme.surface)
    return if (secondaryContrast >= 3f) scheme.secondary else scheme.primary
}

private fun contrastRatio(a: Color, b: Color): Float {
    fun channel(v: Float): Float = if (v <= 0.03928f) {
        v / 12.92f
    } else {
        ((v + 0.055f) / 1.055f).pow(2.4f)
    }

    fun luminance(color: Color): Float {
        val r = channel(color.red)
        val g = channel(color.green)
        val b = channel(color.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    val l1 = luminance(a)
    val l2 = luminance(b)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

@Composable
private fun ShellPearlMiniIcon(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        drawCircle(
            color = scheme.onPrimary,
            radius = size.minDimension * 0.36f,
            center = Offset(size.width / 2f, size.height / 2f)
        )

        drawCircle(
            color = scheme.primary.copy(alpha = 0.42f),
            radius = size.minDimension * 0.16f,
            center = Offset(size.width * 0.60f, size.height * 0.38f)
        )
    }
}

private enum class ShellAnimalIcon { MINNOW, SEAHORSE, MANTA, WHALE, OCTOPUS, JELLYFISH, TURTLE, SHARK, DOLPHIN, SQUID, STARFISH, URCHIN, EEL, FISH }

@Composable
private fun ShellObjectIcon(
    iconKey: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val animalIcon = when {
        "minnow" in iconKey -> ShellAnimalIcon.MINNOW
        "seahorse" in iconKey -> ShellAnimalIcon.SEAHORSE
        "manta" in iconKey -> ShellAnimalIcon.MANTA
        "whale" in iconKey -> ShellAnimalIcon.WHALE
        "octopus" in iconKey -> ShellAnimalIcon.OCTOPUS
        "jellyfish" in iconKey -> ShellAnimalIcon.JELLYFISH
        "turtle" in iconKey -> ShellAnimalIcon.TURTLE
        "shark" in iconKey || "megalodon" in iconKey -> ShellAnimalIcon.SHARK
        "dolphin" in iconKey || "orca" in iconKey -> ShellAnimalIcon.DOLPHIN
        "squid" in iconKey || "kraken" in iconKey || "leviathan" in iconKey -> ShellAnimalIcon.SQUID
        "starfish" in iconKey -> ShellAnimalIcon.STARFISH
        "urchin" in iconKey -> ShellAnimalIcon.URCHIN
        "eel" in iconKey || "snake" in iconKey -> ShellAnimalIcon.EEL
        "creature_icon" in iconKey || "fish" in iconKey || "tang" in iconKey || "seal" in iconKey || "otter" in iconKey || "penguin" in iconKey -> ShellAnimalIcon.FISH
        else -> null
    }
    val vector = when {
        animalIcon != null -> null
        "kelp" in iconKey || "curtain" in iconKey -> Icons.Outlined.Grass
        "bubble" in iconKey || "current" in iconKey -> Icons.Outlined.Waves
        "coral" in iconKey || "perch" in iconKey -> Icons.Outlined.FilterVintage
        else -> Icons.Outlined.Diamond
    }
    Surface(shape = CircleShape, color = scheme.primary.copy(alpha = 0.16f), modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            if (animalIcon != null) {
                ShellAnimalCanvasIcon(animalIcon, Modifier.fillMaxSize().padding(5.dp))
            } else if (vector != null) {
                Icon(imageVector = vector, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ShellAnimalCanvasIcon(
    animalIcon: ShellAnimalIcon,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val ink = scheme.primary
        val accent = scheme.primary.copy(alpha = 0.55f)

        when (animalIcon) {
            ShellAnimalIcon.MINNOW -> {
                drawOval(ink, topLeft = Offset(w * 0.22f, h * 0.34f), size = Size(w * 0.44f, h * 0.30f))
                drawPath(Path().apply {
                    moveTo(w * 0.20f, h * 0.50f)
                    lineTo(w * 0.02f, h * 0.34f)
                    lineTo(w * 0.02f, h * 0.66f)
                    close()
                }, ink)
                drawCircle(scheme.surface, radius = w * 0.035f, center = Offset(w * 0.58f, h * 0.44f))
                drawOval(accent, topLeft = Offset(w * 0.66f, h * 0.24f), size = Size(w * 0.18f, h * 0.12f))
                drawOval(accent, topLeft = Offset(w * 0.72f, h * 0.62f), size = Size(w * 0.20f, h * 0.12f))
            }
            ShellAnimalIcon.SEAHORSE -> {
                drawCircle(ink, radius = w * 0.17f, center = Offset(w * 0.56f, h * 0.24f))
                drawLine(ink, Offset(w * 0.64f, h * 0.25f), Offset(w * 0.86f, h * 0.20f), strokeWidth = w * 0.09f)
                drawPath(Path().apply {
                    moveTo(w * 0.54f, h * 0.36f)
                    cubicTo(w * 0.30f, h * 0.44f, w * 0.38f, h * 0.78f, w * 0.58f, h * 0.70f)
                    cubicTo(w * 0.78f, h * 0.62f, w * 0.68f, h * 0.48f, w * 0.54f, h * 0.56f)
                }, ink, style = Stroke(width = w * 0.13f))
                drawCircle(scheme.surface, radius = w * 0.032f, center = Offset(w * 0.61f, h * 0.20f))
                drawLine(accent, Offset(w * 0.37f, h * 0.48f), Offset(w * 0.18f, h * 0.40f), strokeWidth = w * 0.08f)
            }
            ShellAnimalIcon.MANTA -> {
                drawPath(Path().apply {
                    moveTo(w * 0.50f, h * 0.22f)
                    cubicTo(w * 0.20f, h * 0.30f, w * 0.08f, h * 0.58f, w * 0.02f, h * 0.74f)
                    cubicTo(w * 0.28f, h * 0.66f, w * 0.38f, h * 0.62f, w * 0.50f, h * 0.78f)
                    cubicTo(w * 0.62f, h * 0.62f, w * 0.72f, h * 0.66f, w * 0.98f, h * 0.74f)
                    cubicTo(w * 0.92f, h * 0.58f, w * 0.80f, h * 0.30f, w * 0.50f, h * 0.22f)
                    close()
                }, ink)
                drawLine(accent, Offset(w * 0.50f, h * 0.72f), Offset(w * 0.50f, h * 0.96f), strokeWidth = w * 0.05f)
            }
            ShellAnimalIcon.WHALE -> {
                drawOval(ink, topLeft = Offset(w * 0.12f, h * 0.34f), size = Size(w * 0.68f, h * 0.34f))
                drawPath(Path().apply {
                    moveTo(w * 0.78f, h * 0.50f)
                    lineTo(w * 0.98f, h * 0.30f)
                    lineTo(w * 0.92f, h * 0.50f)
                    lineTo(w * 0.98f, h * 0.70f)
                    close()
                }, ink)
                drawCircle(scheme.surface, radius = w * 0.03f, center = Offset(w * 0.24f, h * 0.44f))
                drawLine(accent, Offset(w * 0.36f, h * 0.34f), Offset(w * 0.44f, h * 0.18f), strokeWidth = w * 0.05f)
                drawLine(accent, Offset(w * 0.44f, h * 0.18f), Offset(w * 0.54f, h * 0.34f), strokeWidth = w * 0.05f)
            }
            ShellAnimalIcon.OCTOPUS -> {
                drawOval(ink, topLeft = Offset(w * 0.26f, h * 0.14f), size = Size(w * 0.48f, h * 0.42f))
                listOf(0.20f, 0.36f, 0.52f, 0.68f).forEach { x ->
                    drawLine(ink, Offset(w * (x + 0.06f), h * 0.52f), Offset(w * x, h * 0.86f), strokeWidth = w * 0.08f)
                }
                drawCircle(scheme.surface, radius = w * 0.035f, center = Offset(w * 0.42f, h * 0.32f))
                drawCircle(scheme.surface, radius = w * 0.035f, center = Offset(w * 0.58f, h * 0.32f))
            }
            ShellAnimalIcon.JELLYFISH -> {
                drawArc(ink, 180f, 180f, true, topLeft = Offset(w * 0.20f, h * 0.18f), size = Size(w * 0.60f, h * 0.46f))
                listOf(0.28f, 0.42f, 0.56f, 0.70f).forEach { x -> drawLine(accent, Offset(w * x, h * 0.48f), Offset(w * (x - 0.05f), h * 0.88f), strokeWidth = w * 0.045f) }
            }
            ShellAnimalIcon.TURTLE -> {
                drawOval(ink, Offset(w * 0.26f, h * 0.24f), Size(w * 0.48f, h * 0.42f))
                drawOval(accent, Offset(w * 0.42f, h * 0.08f), Size(w * 0.16f, h * 0.16f))
                listOf(0.18f to 0.30f, 0.74f to 0.30f, 0.18f to 0.62f, 0.74f to 0.62f).forEach { (x,y) -> drawOval(accent, Offset(w*x,h*y), Size(w*0.18f,h*0.12f)) }
            }
            ShellAnimalIcon.SHARK -> {
                drawOval(ink, Offset(w * 0.16f, h * 0.38f), Size(w * 0.62f, h * 0.24f))
                drawPath(Path().apply { moveTo(w*0.72f,h*0.50f); lineTo(w*0.98f,h*0.30f); lineTo(w*0.90f,h*0.50f); lineTo(w*0.98f,h*0.70f); close() }, ink)
                drawPath(Path().apply { moveTo(w*0.42f,h*0.38f); lineTo(w*0.50f,h*0.12f); lineTo(w*0.58f,h*0.40f); close() }, accent)
            }
            ShellAnimalIcon.DOLPHIN -> {
                drawArc(ink, 195f, 205f, false, topLeft = Offset(w*0.14f,h*0.18f), size = Size(w*0.72f,h*0.52f), style = Stroke(width = w*0.16f))
                drawPath(Path().apply { moveTo(w*0.76f,h*0.43f); lineTo(w*0.98f,h*0.28f); lineTo(w*0.90f,h*0.48f); lineTo(w*0.98f,h*0.66f); close() }, ink)
            }
            ShellAnimalIcon.SQUID -> {
                drawOval(ink, Offset(w*0.34f,h*0.10f), Size(w*0.32f,h*0.42f))
                repeat(5) { i -> drawLine(ink, Offset(w*(0.36f+i*0.07f), h*0.50f), Offset(w*(0.22f+i*0.14f), h*0.90f), strokeWidth = w*0.055f) }
            }
            ShellAnimalIcon.STARFISH -> {
                val path = Path(); repeat(10) { i -> val r= if (i%2==0) .43f else .18f; val a=(-90+i*36)*Math.PI/180; val x=w*.5f+Math.cos(a).toFloat()*w*r; val y=h*.5f+Math.sin(a).toFloat()*h*r; if(i==0) path.moveTo(x,y) else path.lineTo(x,y) }; path.close(); drawPath(path, ink)
            }
            ShellAnimalIcon.URCHIN -> {
                repeat(14) { i -> val a=i*6.28f/14f; drawLine(ink, Offset(w*.5f,h*.5f), Offset(w*(.5f+kotlin.math.cos(a)*.43f), h*(.5f+kotlin.math.sin(a)*.43f)), strokeWidth=w*.035f) }
                drawCircle(ink, w*.24f, Offset(w*.5f,h*.5f))
            }
            ShellAnimalIcon.EEL -> {
                drawArc(ink, 180f, 240f, false, topLeft = Offset(w*.10f,h*.20f), size=Size(w*.76f,h*.58f), style=Stroke(width=w*.13f))
                drawCircle(ink, w*.10f, Offset(w*.76f,h*.38f))
            }
            ShellAnimalIcon.FISH -> {
                drawOval(ink, topLeft = Offset(w * 0.20f, h * 0.34f), size = Size(w * 0.52f, h * 0.30f))
                drawPath(Path().apply { moveTo(w*0.18f,h*0.50f); lineTo(w*0.02f,h*0.34f); lineTo(w*0.02f,h*0.66f); close() }, ink)
                drawPath(Path().apply { moveTo(w*0.54f,h*0.34f); lineTo(w*0.62f,h*0.16f); lineTo(w*0.66f,h*0.38f); close() }, accent)
                drawCircle(scheme.surface, radius = w * 0.03f, center = Offset(w * 0.60f, h * 0.44f))
            }
        }
    }
}

@Composable
private fun TurtleShellCardPattern(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = scheme.secondary.copy(alpha = 0.08f),
            topLeft = Offset(w * 0.08f, -h * 0.30f),
            size = Size(w * 0.84f, h * 1.30f)
        )

        val seamColor = scheme.onSurface.copy(alpha = 0.10f)

        val center = Path().apply {
            moveTo(w * 0.50f, 0f)
            cubicTo(
                w * 0.46f,
                h * 0.35f,
                w * 0.54f,
                h * 0.60f,
                w * 0.50f,
                h
            )
        }

        drawPath(
            path = center,
            color = seamColor,
            style = Stroke(width = 2f)
        )

        repeat(3) { index ->
            val y = h * (0.28f + index * 0.22f)

            val band = Path().apply {
                moveTo(w * 0.10f, y)
                cubicTo(
                    w * 0.32f,
                    y - h * 0.08f,
                    w * 0.44f,
                    y + h * 0.05f,
                    w * 0.50f,
                    y
                )
                cubicTo(
                    w * 0.56f,
                    y + h * 0.05f,
                    w * 0.68f,
                    y - h * 0.08f,
                    w * 0.90f,
                    y
                )
            }

            drawPath(
                path = band,
                color = seamColor,
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
