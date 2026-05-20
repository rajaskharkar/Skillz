package com.kingkharnivore.skillz.ui.screen.shell.theblue

import com.kingkharnivore.skillz.ui.screen.shell.*
private fun TheBlueRoomScreen(
    uiState: ShellUiState,
    onDisplayInFocus: (String, String) -> Unit,
    onGrowCreature: (String) -> Unit,
    onReleaseCreature: (String) -> Unit,
    onEncounterBeyondBlue: (String, List<String>) -> Unit,
    onOpenChest: () -> Unit
) {
    val theBlueState = remember(uiState.finds, uiState.focusPlacements) {
        buildTheBlueUiState(uiState.finds, uiState.focusPlacements)
    }
    var selectedAnimal by remember { mutableStateOf<TheBlueAnimalGroupUiModel?>(null) }
    var releaseCandidate by remember { mutableStateOf<TheBlueAnimalGroupUiModel?>(null) }
    var showBeyondBlue by remember { mutableStateOf(false) }
    var beyondBlueInitialZone by remember { mutableStateOf(TheBlueZoneId.SUNLIT_REEF) }
    var entryNewAnimalFindIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var railNavigationJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(theBlueState.newAnimalCount, theBlueState.zones) {
        if (entryNewAnimalFindIds.isEmpty() && theBlueState.newAnimalCount > 0) {
            entryNewAnimalFindIds = theBlueState.zones
                .flatMap { zone -> zone.animals.filter { it.isNew }.map { it.findId } }
                .toSet()
        }
    }
    val pageCount = if (theBlueState.isEmpty) 1 else theBlueState.zones.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val activeZone by remember(pagerState) {
        derivedStateOf { theBlueZoneForPage(pagerState.currentPage) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(shellBackground())
    ) {
        val pageHeight = maxHeight
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (theBlueState.isEmpty) {
                TheBlueEmptyOceanPage(pageHeight = pageHeight)
            } else {
                val zone = theBlueState.zones[page]
                TheBlueZonePage(
                    zone = zone,
                    state = theBlueState,
                    pageHeight = pageHeight,
                    showRoomHeader = zone.zoneId == TheBlueZoneId.SUNLIT_REEF,
                    entryNewAnimalFindIds = entryNewAnimalFindIds,
                    onZoneBeyondBlue = {
                        beyondBlueInitialZone = zone.zoneId
                        showBeyondBlue = true
                    },
                    onAnimalClick = { selectedAnimal = it }
                )
            }
        }

        if (!theBlueState.isEmpty) {
            TheBlueDepthRail(
                zones = theBlueState.zones.map { it.zoneId },
                activeZone = activeZone,
                onZoneClick = { target ->
                    railNavigationJob?.cancel()
                    railNavigationJob = scope.launch {
                        for (zone in theBlueSequentialNavigationPath(theBlueZoneForPage(pagerState.currentPage), target)) {
                            pagerState.animateScrollToPage(zone.depthOrder())
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }
    }

    selectedAnimal?.let { animal ->
        TheBlueAnimalDetailSheet(
            animal = animal,
            focusSlotId = remember(uiState.focusPlacements, animal.findId) {
                firstOpenFocusSlotFor(animal.findId, uiState)
            },
            pearlBalance = uiState.pearlBalance,
            onDismiss = { selectedAnimal = null },
            onGrow = { instanceId ->
                onGrowCreature(instanceId)
                selectedAnimal = null
            },
            onRelease = {
                releaseCandidate = animal
                selectedAnimal = null
            },
            onBeyondBlue = {
                beyondBlueInitialZone = animal.zoneId
                showBeyondBlue = true
                selectedAnimal = null
            },
            onDisplayInFocus = { instanceId, slotId ->
                onDisplayInFocus(instanceId, slotId)
                selectedAnimal = null
            },
            onOpenChest = {
                selectedAnimal = null
                onOpenChest()
            },
            firstRestingInstanceId = remember(uiState.finds, uiState.focusPlacements, animal.findId) {
                firstRestingInstanceId(animal.findId, uiState)
            }
        )
    }

    releaseCandidate?.let { animal ->
        ReleaseCreatureConfirmationSheet(
            animal = animal,
            onDismiss = { releaseCandidate = null },
            onConfirm = { instanceId ->
                onReleaseCreature(instanceId)
                releaseCandidate = null
            }
        )
    }

    if (showBeyondBlue) {
        BeyondBlueEncounterSheet(
            pearlBalance = uiState.pearlBalance,
            initialZone = beyondBlueInitialZone,
            activeAnimalInstances = uiState.finds.filter {
                it.creatureStatus == CreatureStatus.ACTIVE && ShellContentCatalog.find(it.findId)?.kind == ShellRewardKind.ANIMAL
            },
          onDismiss = { showBeyondBlue = false },
            onEncounter = { targetCreatureId, selectedIds ->
                onEncounterBeyondBlue(targetCreatureId, selectedIds)
                showBeyondBlue = false
            }
        )
    }
}

@Composable
private fun TheBlueEmptyOceanPage(
    pageHeight: Dp
) {
    val scheme = MaterialTheme.colorScheme
    val headerDescription = stringResource(R.string.the_blue_header_a11y)
    val transition = rememberInfiniteTransition(label = "the-blue-empty-motion")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
        label = "empty-water-drift"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pageHeight)
            .semantics { contentDescription = headerDescription }
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawTheBlueWaterBackground(TheBlueZoneId.SUNLIT_REEF, scheme, drift)
            drawSunlitReefEnvironment(scheme, drift, animalDensity = 0)
            repeat(12) { index ->
                val x = ((index * 67f + drift * size.width * 0.35f) % (size.width + 60f)) - 30f
                val y = size.height - ((index * 43f + drift * size.height) % size.height)
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.10f + (index % 3) * 0.02f),
                    radius = 3f + (index % 4),
                    center = Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TheBlueOverlaySurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.shell_room_the_blue_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.shell_room_the_blue_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.shell_room_the_blue_empty_body),
                        color = scheme.onSurface.copy(alpha = 0.78f)
                    )
                    Text(
                        text = stringResource(R.string.the_blue_empty_water_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TheBlueZonePage(
    zone: TheBlueZoneUiModel,
    state: TheBlueUiState,
    pageHeight: Dp,
    showRoomHeader: Boolean,
    entryNewAnimalFindIds: Set<String>,
    onZoneBeyondBlue: () -> Unit,
    onAnimalClick: (TheBlueAnimalGroupUiModel) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val title = zoneTitle(zone.zoneId)
    val subtitle = zoneSubtitle(zone.zoneId)
    val beyondBlueCtaA11y = stringResource(R.string.beyond_blue_encounter_cta)
    val animalSummary = zoneAnimalSummary(zone)
    val zoneDescription = stringResource(R.string.the_blue_zone_scene_a11y, title, subtitle, animalSummary)
    val zoneHasNewArrival = zone.animals.any { it.isNew || it.findId in entryNewAnimalFindIds }
    val transition = rememberInfiniteTransition(label = "the-blue-${zone.zoneId.name.lowercase()}-motion")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(16000 + zone.zoneId.depthOrder() * 7000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "zone-drift"
    )
    val mantaLoop by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "manta-offscreen-loop"
    )
    val whaleLoop by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(32000, easing = LinearEasing), RepeatMode.Restart),
        label = "whale-offscreen-loop"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pageHeight)
            .semantics { contentDescription = zoneDescription }
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawTheBlueWaterBackground(zone.zoneId, scheme, drift)
            drawZoneEnvironment(zone.zoneId, scheme, drift, zone.animals.sumOf { it.totalCount })
            drawZoneAnimals(zone, scheme, drift, mantaLoop, whaleLoop)
            if (zoneHasNewArrival) {
                drawRect(scheme.secondary.copy(alpha = 0.045f))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 22.dp, top = 20.dp, end = 76.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TheBlueOverlaySurface {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (showRoomHeader) {
                        Text(
                            text = stringResource(R.string.shell_room_the_blue_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.shell_room_the_blue_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurface.copy(alpha = 0.74f)
                        )
                        Text(
                            text = stringResource(
                                R.string.the_blue_stat_row,
                                state.totalAnimals,
                                state.speciesCount,
                                zoneTitle(state.deepestZoneId ?: TheBlueZoneId.SUNLIT_REEF)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface.copy(alpha = 0.76f)
                    )
                }
            }
            TheBlueOverlaySurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onZoneBeyondBlue)
                    .semantics {
                        role = Role.Button
                        contentDescription = beyondBlueCtaA11y
                    }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.beyond_blue_encounter_cta),
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.beyond_blue_discover_depth_copy),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface.copy(alpha = 0.72f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 78.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (zone.animals.isEmpty()) {
                TheBlueOverlaySurface {
                    Text(
                        text = stringResource(R.string.the_blue_zone_waiting),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface.copy(alpha = 0.70f)
                    )
                }
            } else {
                zone.animals.forEach { animal ->
                    TheBlueAnimalOverlayChip(
                        animal = animal,
                        isNewArrival = animal.isNew || animal.findId in entryNewAnimalFindIds,
                        onClick = { onAnimalClick(animal) }
                    )
                }
            }
        }
    }
}

@Composable
private fun zoneAnimalSummary(zone: TheBlueZoneUiModel): String {
    if (zone.animals.isEmpty()) return stringResource(R.string.the_blue_zone_waiting)
    val labels = mutableListOf<String>()
    for (animal in zone.animals) {
        labels += stringResource(R.string.the_blue_animal_count, findName(animal.findId), animal.totalCount)
    }
    return labels.joinToString()
}

@Composable
private fun TheBlueOverlaySurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = scheme.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.14f)),
        modifier = modifier,
        content = { Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) { content() } }
    )
}

@Composable
private fun TheBlueAnimalOverlayChip(
    animal: TheBlueAnimalGroupUiModel,
    isNewArrival: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val name = findName(animal.findId)
    val zone = zoneTitle(animal.zoneId)
    val source = theBlueSourceReason(animal.findId)
    val contentDescription = stringResource(
        R.string.the_blue_animal_overlay_a11y,
        name,
        zone,
        animal.totalCount,
        source
    )
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = scheme.surface.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, if (isNewArrival) scheme.secondary.copy(alpha = 0.70f) else scheme.primary.copy(alpha = 0.18f)),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.the_blue_animal_count, name, animal.totalCount),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.the_blue_animal_zone, zone),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurface.copy(alpha = 0.68f)
            )
            Text(
                text = stringResource(R.string.the_blue_tap_for_details),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            if (isNewArrival) {
                Surface(shape = CircleShape, color = scheme.secondary, modifier = Modifier.size(8.dp), content = {})
            }
        }
    }
}

private fun DrawScope.drawTheBlueWaterBackground(
    zoneId: TheBlueZoneId,
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float
) {
    val depth = zoneId.depthOrder()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                scheme.primary.copy(alpha = 0.24f - depth * 0.025f),
                scheme.background.copy(alpha = 0.18f + depth * 0.10f),
                scheme.onSurface.copy(alpha = 0.04f + depth * 0.045f)
            ),
            startY = 0f,
            endY = size.height
        )
    )
    repeat(3) { ray ->
        val offset = ((drift + ray * 0.23f) % 1f) * size.width * 0.18f
        val path = Path().apply {
            moveTo(size.width * (0.12f + ray * 0.22f) + offset, 0f)
            lineTo(size.width * (0.20f + ray * 0.20f) + offset, size.height)
            lineTo(size.width * (0.30f + ray * 0.20f) + offset, size.height)
            lineTo(size.width * (0.20f + ray * 0.22f) + offset, 0f)
            close()
        }
        drawPath(path, scheme.secondary.copy(alpha = (0.07f - depth * 0.012f).coerceAtLeast(0.018f)))
    }
    repeat(18 - depth * 3) { index ->
        val x = ((index * 83f + drift * size.width * (0.10f + depth * 0.03f)) % (size.width + 70f)) - 35f
        val y = size.height - ((index * 47f + drift * size.height * (0.70f - depth * 0.10f)) % size.height)
        drawCircle(
            color = scheme.primary.copy(alpha = 0.055f + (index % 3) * 0.014f),
            radius = 1.8f + (index % 4),
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawZoneEnvironment(
    zoneId: TheBlueZoneId,
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    animalDensity: Int
) {
    when (zoneId) {
        TheBlueZoneId.SUNLIT_REEF -> drawSunlitReefEnvironment(scheme, drift, animalDensity)
        TheBlueZoneId.DEEPER_REEF -> drawDeeperReefEnvironment(scheme, drift, animalDensity)
        TheBlueZoneId.OPEN_BLUE -> drawOpenBlueEnvironment(scheme, drift)
        TheBlueZoneId.GREAT_BLUE -> drawGreatBlueEnvironment(scheme, drift)
    }
}

private fun DrawScope.drawSunlitReefEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    animalDensity: Int
) {
    val baseY = size.height * 0.82f
    drawOval(scheme.secondary.copy(alpha = 0.11f), Offset(-size.width * 0.10f, baseY), Size(size.width * 1.20f, size.height * 0.34f))
    repeat(6) { i ->
        val rootX = size.width * (0.08f + i * 0.16f)
        val sway = sin((drift * 6.28f + i).toDouble()).toFloat() * 10f
        val height = size.height * (0.12f + (i % 3) * 0.035f)
        drawLine(scheme.primary.copy(alpha = 0.34f), Offset(rootX, size.height), Offset(rootX + sway, size.height - height), strokeWidth = 5f)
        drawCircle(scheme.secondary.copy(alpha = 0.28f), 8f + i, Offset(rootX + sway, size.height - height))
    }
    repeat(5 + min(animalDensity / 8, 4)) { i ->
        val x = size.width * (0.05f + i * 0.20f)
        val y = size.height * (0.78f + (i % 2) * 0.07f)
        drawBranchingCoral(x, y, 36f + (i % 3) * 12f, scheme.secondary.copy(alpha = 0.32f), drift + i * 0.1f)
    }
    repeat(5) { i ->
        drawOval(
            scheme.onSurface.copy(alpha = 0.08f),
            Offset(size.width * (0.12f + i * 0.18f), size.height * (0.88f + (i % 2) * 0.03f)),
            Size(36f + i * 7f, 18f + i * 2f)
        )
    }
}

private fun DrawScope.drawDeeperReefEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    animalDensity: Int
) {
    repeat(4) { i ->
        val x = if (i % 2 == 0) size.width * (0.08f + i * 0.08f) else size.width * (0.78f - i * 0.05f)
        val top = size.height * (0.28f + (i % 2) * 0.08f)
        drawRoundRockColumn(x, top, size.height * 0.70f, 42f + i * 8f, scheme.onSurface.copy(alpha = 0.12f))
        drawBranchingCoral(x + 12f, top + 80f, 46f, scheme.primary.copy(alpha = 0.26f), drift + i)
    }
    val caveX = size.width * 0.62f
    val caveY = size.height * 0.70f
    drawOval(scheme.onSurface.copy(alpha = 0.22f), Offset(caveX, caveY), Size(size.width * 0.26f, size.height * 0.16f))
    drawOval(scheme.background.copy(alpha = 0.35f), Offset(caveX + 16f, caveY + 12f), Size(size.width * 0.18f, size.height * 0.10f))
    repeat(5 + min(animalDensity / 6, 4)) { i ->
        val x = size.width * (0.18f + i * 0.15f)
        val sway = sin((drift * 6.28f + i).toDouble()).toFloat() * 8f
        drawLine(scheme.primary.copy(alpha = 0.18f), Offset(x, 0f), Offset(x + sway, size.height * (0.16f + (i % 3) * 0.04f)), strokeWidth = 4f)
    }
    repeat(7) { i ->
        drawCircle(scheme.secondary.copy(alpha = 0.10f), 2.5f + (i % 2), Offset(size.width * (0.15f + i * 0.11f), size.height * (0.42f + (i % 3) * 0.08f)))
    }
}

private fun DrawScope.drawOpenBlueEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float
) {
    repeat(7) { i ->
        val y = size.height * (0.18f + i * 0.10f)
        val xOffset = sin((drift * 6.28f + i).toDouble()).toFloat() * 28f
        drawLine(
            scheme.primary.copy(alpha = 0.11f),
            Offset(-40f + xOffset, y),
            Offset(size.width + 40f + xOffset, y + 24f),
            strokeWidth = 2.5f
        )
    }
    drawOval(scheme.onSurface.copy(alpha = 0.055f), Offset(size.width * 0.62f, size.height * 0.78f), Size(size.width * 0.45f, size.height * 0.16f))
}

private fun DrawScope.drawGreatBlueEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float
) {
    repeat(5) { i ->
        val y = size.height * (0.18f + i * 0.14f)
        drawLine(scheme.onSurface.copy(alpha = 0.045f), Offset(0f, y), Offset(size.width, y + sin((drift * 6.28f + i).toDouble()).toFloat() * 10f), strokeWidth = 10f)
    }
    repeat(10) { i ->
        val x = ((i * 97f + drift * size.width * 0.04f) % size.width)
        val y = ((i * 61f + drift * size.height * 0.12f) % size.height)
        drawCircle(scheme.secondary.copy(alpha = 0.035f), 1.5f + (i % 2), Offset(x, y))
    }
}

private fun DrawScope.drawZoneAnimals(
    zone: TheBlueZoneUiModel,
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    mantaLoop: Float,
    whaleLoop: Float
) {
    zone.animals.forEach { animal ->
        val accentCount = animal.levelCounts.filter { (it.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: 1) > 1 }.sumOf { it.count }
        val visualScale = CreatureEconomy.animalVisualScale(animal.findId, animal.highestLevel)
        when (animal.findId) {
            ShellContentCatalog.FOCUS_MINNOW -> drawMinnowSchool(animal.totalCount, accentCount, scheme, drift, visualScale)
            ShellContentCatalog.FOCUS_SEAHORSE -> drawSeahorseColony(animal.totalCount, accentCount, scheme, drift, visualScale)
            ShellContentCatalog.FOCUS_OCTOPUS -> drawHiddenOctopus(accentCount + 1, scheme, drift)
            ShellContentCatalog.FOCUS_MANTA -> drawMantaGlides(animal.totalCount, accentCount, scheme, drift, mantaLoop, visualScale)
            ShellContentCatalog.FOCUS_WHALE -> drawWhalePasses(animal.totalCount, accentCount, scheme, drift, whaleLoop, visualScale)
            else -> drawRenderFamilyCreatures(animal, scheme, drift, mantaLoop, whaleLoop, visualScale)
        }
    }
}


private fun DrawScope.drawRenderFamilyCreatures(
    animal: TheBlueAnimalGroupUiModel,
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    mantaLoop: Float,
    whaleLoop: Float,
    levelScale: Float
) {
    val definition = CreatureCatalog.get(animal.findId) ?: return
    val visible = representativeVisibleCount(animal.totalCount, maxVisible = 5)
    repeat(visible) { i ->
        val progress = ((drift * (0.55f + i * 0.05f)) + i * 0.19f) % 1f
        val x = offscreenHorizontalPassX(progress, size.width, 82f, 36f, i % 2 == 0)
        val y = size.height * (0.22f + ((i * 17) % 52) / 100f)
        val scale = (0.85f + (i % 3) * 0.10f) * levelScale
        when (definition.renderFamily.key) {
            "ray" -> drawManta(Offset(x, y), scale * 0.72f, drift + i, false, scheme)
            "whale" -> drawWhale(Offset(x, y), scale * 0.58f, drift + i, false, scheme)
            "octopus" -> drawOctopus(Offset(size.width * 0.62f, size.height * 0.72f), drift, false, scheme)
            else -> drawGenericFish(Offset(x, y), scale, drift + i, scheme, definition.renderFamily.key)
        }
    }
}

private fun DrawScope.drawGenericFish(origin: Offset, scale: Float, drift: Float, scheme: androidx.compose.material3.ColorScheme, familyKey: String) {
    val ink = when (familyKey) {
        "jellyfish", "giant_tentacle", "legendary" -> scheme.secondary.copy(alpha = 0.66f)
        "shark", "orca", "anglerfish" -> scheme.onSurface.copy(alpha = 0.52f)
        else -> scheme.primary.copy(alpha = 0.60f)
    }
    val w = 34f * scale
    val h = 18f * scale
    val bob = sin((drift * 6.28f).toDouble()).toFloat() * 5f
    if (familyKey == "jellyfish") {
        drawCircle(ink, w * 0.42f, Offset(origin.x, origin.y + bob))
        repeat(4) { t -> drawLine(ink, Offset(origin.x - w * .30f + t*w*.20f, origin.y + bob + h*.30f), Offset(origin.x - w * .38f + t*w*.22f, origin.y + bob + h*1.4f), strokeWidth = 2.4f * scale) }
    } else {
        drawOval(ink, Offset(origin.x - w * 0.50f, origin.y - h * 0.50f + bob), Size(w, h))
        drawPath(Path().apply { moveTo(origin.x - w*.50f, origin.y + bob); lineTo(origin.x - w*.82f, origin.y - h*.50f + bob); lineTo(origin.x - w*.82f, origin.y + h*.50f + bob); close() }, ink)
        drawPath(Path().apply { moveTo(origin.x, origin.y - h*.48f + bob); lineTo(origin.x + w*.12f, origin.y - h*1.05f + bob); lineTo(origin.x + w*.22f, origin.y - h*.35f + bob); close() }, scheme.secondary.copy(alpha = 0.38f))
    }
}

private fun DrawScope.drawMinnowSchool(count: Int, accentCount: Int, scheme: androidx.compose.material3.ColorScheme, drift: Float, levelScale: Float = 1f) {
    val visible = representativeVisibleCount(count, maxVisible = 12)
    repeat(visible) { i ->
        val group = i / 4
        val progress = (drift * (1.05f + group * 0.12f) + i * 0.075f) % 1f
        val wiggle = sin((drift * 18f + i).toDouble()).toFloat()
        val x = progress * (size.width + 140f) - 70f
        val y = size.height * (0.34f + group * 0.12f) + (i % 4) * 20f + wiggle * 8f
        drawMinnow(Offset(x, y), (1f + (i % 3) * 0.08f) * levelScale, wiggle, i < accentCount.coerceAtMost(visible), scheme)
    }
}

private fun DrawScope.drawSeahorseColony(count: Int, accentCount: Int, scheme: androidx.compose.material3.ColorScheme, drift: Float, levelScale: Float = 1f) {
    val visible = representativeVisibleCount(count, maxVisible = 6)
    repeat(visible) { i ->
        val bob = sin((drift * 6.28f + i * 0.9f).toDouble()).toFloat()
        val x = size.width * (0.22f + (i % 3) * 0.16f)
        val y = size.height * (0.46f + (i / 3) * 0.16f) + bob * 14f
        drawSeahorse(Offset(x, y), (1f + (i % 2) * 0.08f) * levelScale, bob, i < accentCount.coerceAtMost(visible), scheme)
    }
}

private fun DrawScope.drawMantaGlides(
    count: Int,
    accentCount: Int,
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    mantaLoop: Float,
    levelScale: Float = 1f
) {
    val visible = representativeVisibleCount(count, maxVisible = 3)
    repeat(visible) { i ->
        val scale = (1.0f + i * 0.16f) * levelScale
        val mantaWidth = 132f * scale
        val progress = (mantaLoop + 0.20f + i * 0.28f) % 1f
        val x = offscreenHorizontalPassX(
            progress = progress,
            screenWidth = size.width,
            animalWidth = mantaWidth,
            margin = 56f,
            leftToRight = true
        )
        val y = size.height * (0.32f + i * 0.18f) + sin((drift * 6.28f + i).toDouble()).toFloat() * 18f
        drawManta(Offset(x, y), scale, drift + i * 0.2f, i < accentCount.coerceAtMost(visible), scheme)
    }
}

private fun DrawScope.drawWhalePasses(
    count: Int,
    accentCount: Int,
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    whaleLoop: Float,
    levelScale: Float = 1f
) {
    val visible = representativeVisibleCount(count, maxVisible = 2)
    repeat(visible) { i ->
        val scale = (1.28f + i * 0.12f) * levelScale
        val whaleWidth = 176f * scale
        val progress = (whaleLoop + 0.22f + i * 0.48f) % 1f
        val x = offscreenHorizontalPassX(
            progress = progress,
            screenWidth = size.width,
            animalWidth = whaleWidth,
            margin = 72f,
            leftToRight = false
        )
        val y = size.height * (0.42f + i * 0.16f) + sin((drift * 6.28f + i).toDouble()).toFloat() * 10f
        drawWhale(Offset(x, y), scale, drift + i, accentCount > 0, scheme)
    }
}

private fun DrawScope.drawHiddenOctopus(accentCount: Int, scheme: androidx.compose.material3.ColorScheme, drift: Float) {
    val pulse = 1f + sin((drift * 6.28f).toDouble()).toFloat() * 0.05f
    val origin = Offset(size.width * 0.70f, size.height * 0.73f)
    drawOctopus(origin, pulse, accentCount > 0, scheme)
}

internal fun representativeVisibleCount(count: Int, maxVisible: Int): Int = when {
    count <= 0 -> 0
    count == 1 -> 1
    count <= 4 -> min(count, maxVisible)
    count <= 14 -> min(6, maxVisible)
    count <= 49 -> min(9, maxVisible)
    else -> maxVisible
}

private fun DrawScope.drawMinnow(origin: Offset, scale: Float, wiggle: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.primary.copy(alpha = if (glowing) 0.82f else 0.64f)
    val fin = scheme.secondary.copy(alpha = if (glowing) 0.58f else 0.36f)
    if (glowing) drawCircle(scheme.secondary.copy(alpha = 0.18f), 22f * scale, origin)
    drawOval(body, Offset(origin.x - 14f * scale, origin.y - 6f * scale), Size(28f * scale, 12f * scale))
    val tail = Path().apply {
        moveTo(origin.x - 13f * scale, origin.y)
        lineTo(origin.x - 26f * scale, origin.y - (8f + wiggle * 3f) * scale)
        lineTo(origin.x - 25f * scale, origin.y + (8f - wiggle * 3f) * scale)
        close()
    }
    drawPath(tail, fin)
    val dorsal = Path().apply {
        moveTo(origin.x - 2f * scale, origin.y - 6f * scale)
        lineTo(origin.x + 5f * scale, origin.y - 13f * scale)
        lineTo(origin.x + 9f * scale, origin.y - 5f * scale)
        close()
    }
    drawPath(dorsal, fin.copy(alpha = fin.alpha * 0.75f))
    drawCircle(scheme.onSurface.copy(alpha = 0.74f), 1.6f * scale, Offset(origin.x + 9f * scale, origin.y - 1.5f * scale))
    drawCircle(scheme.secondary.copy(alpha = 0.32f), 1.7f * scale, Offset(origin.x + 2f * scale, origin.y + 2f * scale))
}

private fun DrawScope.drawSeahorse(origin: Offset, scale: Float, bob: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    if (glowing) drawCircle(scheme.secondary.copy(alpha = 0.16f), 30f * scale, origin)
    val color = scheme.secondary.copy(alpha = 0.58f)
    drawCircle(color, 10f * scale, Offset(origin.x, origin.y - 18f * scale))
    drawCircle(color.copy(alpha = 0.82f), 13f * scale, Offset(origin.x - 2f * scale, origin.y + 2f * scale))
    drawLine(color, Offset(origin.x + 7f * scale, origin.y - 19f * scale), Offset(origin.x + 22f * scale, origin.y - 23f * scale), strokeWidth = 5f * scale)
    val crest = Path().apply {
        moveTo(origin.x - 4f * scale, origin.y - 29f * scale)
        lineTo(origin.x + 2f * scale, origin.y - 38f * scale)
        lineTo(origin.x + 7f * scale, origin.y - 28f * scale)
    }
    drawPath(crest, color, style = Stroke(width = 3f * scale))
    val tail = Path().apply {
        moveTo(origin.x - 4f * scale, origin.y + 14f * scale)
        cubicTo(origin.x - 8f * scale, origin.y + 30f * scale, origin.x + 16f * scale, origin.y + 34f * scale, origin.x + 12f * scale, origin.y + 18f * scale)
    }
    drawPath(tail, color, style = Stroke(width = 4f * scale))
    drawOval(scheme.primary.copy(alpha = 0.24f), Offset(origin.x - 15f * scale, origin.y - (2f + bob) * scale), Size(10f * scale, 16f * scale))
    drawCircle(scheme.onSurface.copy(alpha = 0.72f), 1.7f * scale, Offset(origin.x + 6f * scale, origin.y - 21f * scale))
}

private fun DrawScope.drawManta(origin: Offset, scale: Float, drift: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    val wingPulse = sin((drift * 6.28f).toDouble()).toFloat()
    val wingLift = wingPulse * 7f * scale
    val bodyColor = scheme.primary.copy(alpha = 0.48f)
    val wingColor = scheme.primary.copy(alpha = 0.40f)
    val accent = scheme.secondary.copy(alpha = if (glowing) 0.34f else 0.18f)

    if (glowing) {
        drawOval(
            color = scheme.secondary.copy(alpha = 0.10f),
            topLeft = Offset(origin.x - 94f * scale, origin.y - 54f * scale),
            size = Size(188f * scale, 118f * scale)
        )
    }

    val manta = Path().apply {
        // Top-view / three-quarter-top-view ray silhouette: broad wings first, then body taper.
        moveTo(origin.x, origin.y - 42f * scale)
        cubicTo(origin.x - 18f * scale, origin.y - 45f * scale, origin.x - 55f * scale, origin.y - 42f * scale + wingLift, origin.x - 98f * scale, origin.y - 8f * scale + wingLift)
        cubicTo(origin.x - 66f * scale, origin.y + 2f * scale, origin.x - 36f * scale, origin.y + 24f * scale, origin.x - 10f * scale, origin.y + 42f * scale)
        cubicTo(origin.x - 4f * scale, origin.y + 47f * scale, origin.x + 4f * scale, origin.y + 47f * scale, origin.x + 10f * scale, origin.y + 42f * scale)
        cubicTo(origin.x + 36f * scale, origin.y + 24f * scale, origin.x + 66f * scale, origin.y + 2f * scale, origin.x + 98f * scale, origin.y - 8f * scale - wingLift)
        cubicTo(origin.x + 55f * scale, origin.y - 42f * scale - wingLift, origin.x + 18f * scale, origin.y - 45f * scale, origin.x, origin.y - 42f * scale)
        close()
    }
    drawPath(manta, wingColor)

    val center = Path().apply {
        moveTo(origin.x, origin.y - 36f * scale)
        cubicTo(origin.x - 20f * scale, origin.y - 18f * scale, origin.x - 18f * scale, origin.y + 22f * scale, origin.x, origin.y + 40f * scale)
        cubicTo(origin.x + 18f * scale, origin.y + 22f * scale, origin.x + 20f * scale, origin.y - 18f * scale, origin.x, origin.y - 36f * scale)
        close()
    }
    drawPath(center, bodyColor)

    val underside = Path().apply {
        moveTo(origin.x, origin.y - 18f * scale)
        cubicTo(origin.x - 12f * scale, origin.y - 2f * scale, origin.x - 10f * scale, origin.y + 18f * scale, origin.x, origin.y + 29f * scale)
        cubicTo(origin.x + 10f * scale, origin.y + 18f * scale, origin.x + 12f * scale, origin.y - 2f * scale, origin.x, origin.y - 18f * scale)
        close()
    }
    drawPath(underside, accent)

    // Cephalic-lobe suggestion and small eyes make it read as a manta, not a flat diamond.
    drawLine(bodyColor.copy(alpha = 0.62f), Offset(origin.x - 9f * scale, origin.y - 37f * scale), Offset(origin.x - 24f * scale, origin.y - 48f * scale), strokeWidth = 3f * scale)
    drawLine(bodyColor.copy(alpha = 0.62f), Offset(origin.x + 9f * scale, origin.y - 37f * scale), Offset(origin.x + 24f * scale, origin.y - 48f * scale), strokeWidth = 3f * scale)
    drawCircle(scheme.onSurface.copy(alpha = 0.34f), 1.8f * scale, Offset(origin.x - 9f * scale, origin.y - 25f * scale))
    drawCircle(scheme.onSurface.copy(alpha = 0.34f), 1.8f * scale, Offset(origin.x + 9f * scale, origin.y - 25f * scale))

    val tailSway = sin((drift * 6.28f - 0.8f).toDouble()).toFloat() * 9f * scale
    val tail = Path().apply {
        moveTo(origin.x, origin.y + 38f * scale)
        cubicTo(origin.x + tailSway * 0.25f, origin.y + 72f * scale, origin.x + tailSway, origin.y + 94f * scale, origin.x + tailSway * 0.65f, origin.y + 126f * scale)
    }
    drawPath(tail, bodyColor.copy(alpha = 0.52f), style = Stroke(width = 2.4f * scale))

    if (glowing) {
        drawLine(accent, Offset(origin.x - 74f * scale, origin.y - 6f * scale + wingLift), Offset(origin.x - 18f * scale, origin.y + 22f * scale), strokeWidth = 2f * scale)
        drawLine(accent, Offset(origin.x + 74f * scale, origin.y - 6f * scale - wingLift), Offset(origin.x + 18f * scale, origin.y + 22f * scale), strokeWidth = 2f * scale)
    }
}

private fun DrawScope.drawWhale(origin: Offset, scale: Float, drift: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    val color = scheme.onSurface.copy(alpha = 0.22f)
    val rim = if (glowing) scheme.secondary.copy(alpha = 0.20f) else scheme.primary.copy(alpha = 0.10f)
    drawOval(rim, Offset(origin.x - 118f * scale, origin.y - 38f * scale), Size(220f * scale, 78f * scale))
    drawOval(color, Offset(origin.x - 108f * scale, origin.y - 28f * scale), Size(190f * scale, 56f * scale))
    drawOval(color.copy(alpha = 0.16f), Offset(origin.x - 54f * scale, origin.y + 2f * scale), Size(94f * scale, 22f * scale))
    val tailWave = sin((drift * 6.28f).toDouble()).toFloat() * 8f * scale
    val tail = Path().apply {
        moveTo(origin.x + 78f * scale, origin.y)
        lineTo(origin.x + 126f * scale, origin.y - 25f * scale + tailWave)
        lineTo(origin.x + 114f * scale, origin.y)
        lineTo(origin.x + 128f * scale, origin.y + 25f * scale + tailWave)
        close()
    }
    drawPath(tail, color.copy(alpha = 0.26f))
    drawCircle(scheme.background.copy(alpha = 0.45f), 2.4f * scale, Offset(origin.x - 74f * scale, origin.y - 8f * scale))
}

private fun DrawScope.drawOctopus(origin: Offset, pulse: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    if (glowing) drawCircle(scheme.secondary.copy(alpha = 0.15f), 46f * pulse, origin)
    val color = scheme.secondary.copy(alpha = 0.46f)
    drawOval(color, Offset(origin.x - 22f * pulse, origin.y - 30f * pulse), Size(44f * pulse, 38f * pulse))
    repeat(6) { i ->
        val startX = origin.x - 18f + i * 7f
        val curl = sin((pulse * 4f + i).toDouble()).toFloat() * 8f
        val tentacle = Path().apply {
            moveTo(startX, origin.y + 2f)
            cubicTo(startX - 10f, origin.y + 22f, startX + curl, origin.y + 32f, startX - 4f, origin.y + 44f)
        }
        drawPath(tentacle, color, style = Stroke(width = 4f))
    }
    drawCircle(scheme.onSurface.copy(alpha = 0.75f), 2.4f * pulse, Offset(origin.x + 8f * pulse, origin.y - 14f * pulse))
}

private fun DrawScope.drawBranchingCoral(x: Float, y: Float, height: Float, color: Color, drift: Float) {
    val sway = sin((drift * 6.28f).toDouble()).toFloat() * 5f
    drawLine(color, Offset(x, y), Offset(x + sway, y - height), strokeWidth = 5f)
    drawLine(color, Offset(x + sway * 0.6f, y - height * 0.55f), Offset(x - 16f + sway, y - height * 0.86f), strokeWidth = 4f)
    drawLine(color, Offset(x + sway * 0.7f, y - height * 0.45f), Offset(x + 17f + sway, y - height * 0.78f), strokeWidth = 4f)
}

private fun DrawScope.drawRoundRockColumn(x: Float, top: Float, bottom: Float, width: Float, color: Color) {
    drawOval(color, Offset(x - width / 2f, top), Size(width, bottom - top))
    drawOval(color.copy(alpha = color.alpha * 0.7f), Offset(x - width * 0.65f, top + 60f), Size(width * 1.3f, width * 0.75f))
}


private fun formatMinutesCompact(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val mins = safe % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

private fun TheBlueZoneId.toCreatureZone(): CreatureZone = when (this) {
    TheBlueZoneId.SUNLIT_REEF -> CreatureZone.SUNLIT_REEF
    TheBlueZoneId.DEEPER_REEF -> CreatureZone.DEEPER_REEF
    TheBlueZoneId.OPEN_BLUE -> CreatureZone.OPEN_BLUE
    TheBlueZoneId.GREAT_BLUE -> CreatureZone.GREAT_BLUE
}

private fun theBlueZoneFor(zone: CreatureZone): TheBlueZoneId = when (zone) {
    CreatureZone.SUNLIT_REEF -> TheBlueZoneId.SUNLIT_REEF
    CreatureZone.DEEPER_REEF -> TheBlueZoneId.DEEPER_REEF
    CreatureZone.OPEN_BLUE -> TheBlueZoneId.OPEN_BLUE
    CreatureZone.GREAT_BLUE -> TheBlueZoneId.GREAT_BLUE
}

@Composable
private fun TheBlueAnimalDetailSheet(
    animal: TheBlueAnimalGroupUiModel,
    focusSlotId: String?,
    firstRestingInstanceId: String?,
    pearlBalance: Int,
    onDismiss: () -> Unit,
    onGrow: (String) -> Unit,
    onRelease: () -> Unit,
    onBeyondBlue: () -> Unit,
    onDisplayInFocus: (String, String) -> Unit,
    onOpenChest: () -> Unit
) {
    val name = findName(animal.findId)
    val zone = zoneTitle(animal.zoneId)
    val title = stringResource(R.string.the_blue_animal_count, name, animal.totalCount)
    val source = theBlueEncounteredReason(animal.findId)
    val detailDescription = stringResource(R.string.the_blue_detail_a11y, title, zone, source)
    val growthInstanceId = animal.highestLevelActiveInstanceId ?: animal.firstRestingInstanceId ?: animal.firstActiveInstanceId
    val releaseInstanceId = animal.firstRestingInstanceId ?: animal.firstActiveInstanceId
    val growthCost = CreatureEconomy.growthCostPearls(animal.findId, animal.highestLevel.coerceAtLeast(1))
    val canGrow = growthInstanceId != null && pearlBalance >= growthCost
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .semantics { contentDescription = detailDescription },
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.the_blue_animal_zone, zone),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(source)

            Text(stringResource(R.string.shell_creature_swimming_now, animal.totalCount))
            Text(stringResource(R.string.shell_creature_lifetime_encountered, animal.lifetimeEncounteredCount))
            if (animal.releasedCount > 0) Text(stringResource(R.string.shell_creature_released, animal.releasedCount))
            if (animal.usedBeyondBlueCount > 0) Text(stringResource(R.string.shell_creature_used_beyond_blue, animal.usedBeyondBlueCount))
            Text(stringResource(R.string.shell_creature_highest_level, animal.highestLevel))
            animal.flowTimeValueMinutes?.let { Text(stringResource(R.string.shell_creature_flow_time_value_each, formatMinutesCompact(it))) }
            animal.releaseValuePearls?.let { Text(stringResource(R.string.shell_creature_release_value_each, it)) }

            Text(stringResource(R.string.shell_creature_levels_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (animal.levelCounts.isEmpty()) {
                Text(stringResource(R.string.the_blue_forms_unavailable))
            } else {
                animal.levelCounts.sortedBy { it.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: 0 }
                    .forEach { level -> Text("${level.formStageId} ×${level.count}") }
            }

            Text(stringResource(R.string.the_blue_displayed_in_focus_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(animal.displayedInFocusCount.toString())
            Text(stringResource(R.string.the_blue_resting_in_chest_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(animal.restingCount.toString())

            Text(stringResource(R.string.the_blue_actions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = { growthInstanceId?.let(onGrow) },
                enabled = canGrow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.shell_creature_grow_with_pearls_cost, growthCost))
            }
            if (!canGrow) {
                val missing = (growthCost - pearlBalance).coerceAtLeast(0)
                Text(
                    text = if (growthInstanceId == null) stringResource(R.string.shell_creature_no_active_to_grow) else stringResource(R.string.shell_creature_need_more_pearls_to_grow, missing),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBeyondBlue, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.beyond_blue_encounter_cta))
                }
                OutlinedButton(
                    onClick = onRelease,
                    enabled = releaseInstanceId != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_creature_release_for_pearls))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (firstRestingInstanceId != null && focusSlotId != null) {
                            onDisplayInFocus(firstRestingInstanceId, focusSlotId)
                        }
                    },
                    enabled = firstRestingInstanceId != null && focusSlotId != null,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.the_blue_display_one_in_focus)) }
                OutlinedButton(onClick = onOpenChest, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.the_blue_view_in_chest))
                }
            }
            when (theBlueDisplayDisabledReason(focusSlotId, firstRestingInstanceId)) {
                TheBlueDisplayDisabledReason.NO_FOCUS_SLOT -> Text(
                    stringResource(R.string.the_blue_no_focus_slot, name),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                TheBlueDisplayDisabledReason.NO_RESTING_COPY -> Text(
                    stringResource(R.string.the_blue_no_resting_copy),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                null -> Unit
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ReleaseCreatureConfirmationSheet(
    animal: TheBlueAnimalGroupUiModel,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val name = findName(animal.findId)
    val instanceId = animal.firstRestingInstanceId ?: animal.firstActiveInstanceId
    val releaseValue = animal.releaseValuePearls ?: CreatureEconomy.releaseValuePearls(animal.findId, animal.highestLevel)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.shell_creature_release_confirm_title, name, releaseValue), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.shell_creature_release_confirm_body))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.shell_creature_keep_swimming)) }
                Button(
                    onClick = { instanceId?.let(onConfirm) },
                    enabled = instanceId != null,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.shell_creature_release_for_pearls)) }
            }
        }
    }
}

@Composable
