package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureRealm
import java.text.NumberFormat

/** Both Chest and scene dialogs must describe the same realm completion. */
internal fun creatureCompletionCollectionId(realm: CreatureRealm, includesHabitats: Boolean): String =
    when (realm) {
        CreatureRealm.RED -> "collection_red"
        CreatureRealm.LAND -> if (includesHabitats) "collection_all_land" else "collection_land"
        CreatureRealm.SEA -> if (includesHabitats) "collection_all_waters" else "collection_the_blue"
    }

@Composable
internal fun creatureCompletionText(creatureId: String, includesHabitats: Boolean = false): String =
    stringResource(R.string.level99_preview_completes_realm,
        collectionDisplayName(creatureCompletionCollectionId(CreatureCatalog.require(creatureId).realm, includesHabitats)))

@Composable
internal fun localizedCreatureCount(count: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    return formatter.format(count)
}
