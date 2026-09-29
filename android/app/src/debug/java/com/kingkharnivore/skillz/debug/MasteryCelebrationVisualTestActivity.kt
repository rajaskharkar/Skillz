package com.kingkharnivore.skillz.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.kingkharnivore.skillz.data.model.entity.shell.MasteryCelebrationEventEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.domain.achievement.BadgeRequirement
import com.kingkharnivore.skillz.domain.achievement.CelebrationLifecycle
import com.kingkharnivore.skillz.domain.achievement.CelebrationStage
import com.kingkharnivore.skillz.domain.achievement.CollectionCatalog
import com.kingkharnivore.skillz.domain.achievement.MasteryCelebrationStateMachine
import com.kingkharnivore.skillz.ui.screen.shell.inventory.MasteryCelebrationScreen
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState

/** Debug-only deterministic host used for device screenshots; it never writes fake mastery data. */
class MasteryCelebrationVisualTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val scenario = intent.getStringExtra(EXTRA_SCENARIO) ?: SCENARIO_FIRST_MINNOW
        val initialPage = intent.getIntExtra(EXTRA_PAGE, 1).coerceIn(1, 3)
        setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                var event by remember(scenario, initialPage) {
                    mutableStateOf(MasteryVisualFixtures.event(scenario, initialPage))
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MasteryCelebrationScreen(
                        event = event,
                        uiState = ShellUiState(masteryCelebration = event),
                        onBegin = {},
                        onAdvance = {
                            val stage = CelebrationStage.valueOf(event.presentationStage)
                            val transition = MasteryCelebrationStateMachine.advance(stage)
                            event = event.copy(
                                lifecycleState = transition.lifecycle.name,
                                presentationStage = transition.stage.name
                            )
                        },
                        onPrevious = {
                            val stage = CelebrationStage.valueOf(event.presentationStage)
                            val transition = MasteryCelebrationStateMachine.previous(stage)
                            event = event.copy(
                                lifecycleState = transition.lifecycle.name,
                                presentationStage = transition.stage.name
                            )
                        },
                        onComplete = {},
                        onPin = { _, _ -> },
                        onDismissPinReplacement = {},
                        onUnpin = {},
                        onTrack = {},
                        onUntrack = {},
                        onNavigate = {}
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENARIO = "scenario"
        const val EXTRA_PAGE = "page"
        const val SCENARIO_FIRST_MINNOW = "first_minnow"
        const val SCENARIO_SECOND_MINNOW = "second_minnow"
        const val SCENARIO_MINNOW_MILESTONE = "minnow_milestone"
        const val SCENARIO_FIRST_SEAHORSE = "first_seahorse"
    }
}

private object MasteryVisualFixtures {
    fun event(scenario: String, page: Int): MasteryCelebrationEventEntity {
        val fixture = when (scenario) {
            MasteryCelebrationVisualTestActivity.SCENARIO_SECOND_MINNOW -> Fixture(
                speciesId = ShellContentCatalog.FOCUS_MINNOW,
                speciesCount = 2,
                totalMasteries = 2,
                uniqueSpecies = 1,
                regionMastered = 1,
                blueMastered = 1,
                allMastered = 1,
                newlyEarned = "",
                milestones = ""
            )
            MasteryCelebrationVisualTestActivity.SCENARIO_MINNOW_MILESTONE -> Fixture(
                speciesId = ShellContentCatalog.FOCUS_MINNOW,
                speciesCount = 5,
                totalMasteries = 5,
                uniqueSpecies = 1,
                regionMastered = 1,
                blueMastered = 1,
                allMastered = 1,
                newlyEarned = "",
                milestones = "mastery_species_${ShellContentCatalog.FOCUS_MINNOW}:5,mastery_circle:5"
            )
            MasteryCelebrationVisualTestActivity.SCENARIO_FIRST_SEAHORSE -> Fixture(
                speciesId = ShellContentCatalog.FOCUS_SEAHORSE,
                speciesCount = 1,
                totalMasteries = 2,
                uniqueSpecies = 2,
                regionMastered = 1,
                blueMastered = 2,
                allMastered = 2,
                newlyEarned = "mastery_species_${ShellContentCatalog.FOCUS_SEAHORSE}",
                milestones = "mastery_species_${ShellContentCatalog.FOCUS_SEAHORSE}:1"
            )
            else -> Fixture(
                speciesId = ShellContentCatalog.FOCUS_MINNOW,
                speciesCount = 1,
                totalMasteries = 1,
                uniqueSpecies = 1,
                regionMastered = 1,
                blueMastered = 1,
                allMastered = 1,
                newlyEarned = listOf(
                    "mastery_first",
                    "mastery_species_${ShellContentCatalog.FOCUS_MINNOW}",
                    "mastery_variety",
                    "mastery_circle"
                ).joinToString(","),
                milestones = "mastery_species_${ShellContentCatalog.FOCUS_MINNOW}:1,mastery_circle:1"
            )
        }
        val species = CreatureCatalog.require(fixture.speciesId)
        val stage = when (page) {
            1 -> CelebrationStage.LEVEL_TRANSITION
            2 -> CelebrationStage.MASTERY_REVEAL
            else -> CelebrationStage.FINAL_SUMMARY
        }
        return MasteryCelebrationEventEntity(
            eventId = "visual:$scenario",
            transactionId = "visual:$scenario",
            creatureInstanceId = "visual-creature:$scenario",
            speciesId = fixture.speciesId,
            artworkKey = species.staticIconKey,
            regionId = species.primaryProgressCollectionId,
            sourceId = species.sourceType.name,
            previousLevel = 98,
            newLevel = 99,
            speciesMasteryCount = fixture.speciesCount,
            totalMasteries = fixture.totalMasteries,
            uniqueMasteredSpecies = fixture.uniqueSpecies,
            regionalDiscovered = fixture.regionMastered,
            regionalTotal = completionistTotal(species.primaryProgressCollectionId),
            regionalMastered = fixture.regionMastered,
            regionalCollectorEarned = false,
            regionalCompletionistEarned = false,
            blueMastered = fixture.blueMastered,
            blueTotal = completionistTotal("collection_the_blue"),
            stillwaterMastered = 0,
            stillwaterTotal = completionistTotal("collection_stillwater"),
            allWatersMastered = fixture.allMastered,
            allWatersTotal = completionistTotal("collection_all_waters"),
            newlyEarnedBadgeIds = fixture.newlyEarned,
            advancedBadgeIds = "",
            milestonesReached = fixture.milestones,
            originDestination = "BLUE",
            createdAt = 1L,
            lifecycleState = if (page == 3) CelebrationLifecycle.SUMMARY_REACHED.name
                else CelebrationLifecycle.PRESENTING.name,
            presentationStage = stage.name
        )
    }

    private fun completionistTotal(collectionId: String): Int =
        CollectionCatalog.byId.getValue(collectionId)
            .eligibleRoster(BadgeRequirement.COMPLETIONIST)
            .size

    private data class Fixture(
        val speciesId: String,
        val speciesCount: Int,
        val totalMasteries: Int,
        val uniqueSpecies: Int,
        val regionMastered: Int,
        val blueMastered: Int,
        val allMastered: Int,
        val newlyEarned: String,
        val milestones: String
    )
}
