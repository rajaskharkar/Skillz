package com.kingkharnivore.skillz.ui.screen.shell.badges

import com.kingkharnivore.skillz.ui.screen.shell.*
private fun BadgesScreen(uiState: ShellUiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            RoomHeader(
                title = R.string.shell_badges_title,
                body = R.string.shell_badges_body
            )
        }

        items(uiState.badges) { badge ->
            val def = ShellContentCatalog.badge(badge.badgeId) ?: return@items
            val title = stringResource(def.titleRes)
            val badgeDescription = stringResource(R.string.shell_badge_a11y, title, badge.count)

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.semantics {
                    contentDescription = badgeDescription
                }
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.MilitaryTech,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.shell_badge_row_title, title, badge.count)) },
                    supportingContent = {
                        Text(stringResource(def.descriptionRes))
                    }
                )
            }
        }
    }
}

@Composable
