package com.kingkharnivore.skillz.domain.green

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.*
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.viewmodel.green.GreenUiState
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class GreenUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun emptyWorldHasAllSixEnvironmentsAndNoRetiredRoom() {
        compose.setContent { SkillzTheme { Surface { GreenScreen(GreenUiState()) } } }
        compose.onNodeWithText("The Green").assertIsDisplayed()
        compose.onNodeWithText("Choose a Seed").assertExists()
        listOf("Garden","Woodlands","Rainforest","Wetlands","Drylands","Highlands").forEach { compose.onAllNodesWithText(it).onFirst().assertExists() }
        compose.onNodeWithText("Stillwater").assertDoesNotExist()
        compose.onNodeWithText("Choose a Seed").performClick()
        compose.onNodeWithText("Daisy").performClick()
        compose.onNodeWithText("Plant Seed").assertIsNotEnabled()
        compose.onNodeWithText("Drops needed: 1800").assertExists()
    }
    @Test fun affordableSeedRequestsOneStableCommandAndShowsDeterministicPrice() {
        val tokens=mutableListOf<String>()
        compose.setContent { SkillzTheme { Surface { GreenScreen(GreenUiState(drops=1800),onPlant={id,token->assertEquals("daisy",id);tokens+=token},initialSection=GreenSection.CATALOGUE) } } }
        compose.onNodeWithText("Daisy").performClick()
        compose.onNodeWithText("Plant Seed").assertIsEnabled().performClick().performClick()
        compose.runOnIdle { assertEquals(2,tokens.size);assertEquals(1,tokens.distinct().size) }
    }
    @Test fun mixedWorldContainsIndependentDuplicatesAndAccessibleStagesInDarkMode() {
        compose.setContent { SkillzTheme(darkTheme=true) { Surface { GreenScreen(GreenPreviewFixtures.mixed(GreenEnvironment.GARDEN)) } } }
        compose.onNodeWithContentDescription("Daisy, Level 1, Sprout").assertExists()
        compose.onNodeWithTag("green-specimens:garden").performScrollToIndex(1)
        compose.onNodeWithContentDescription("Daisy, Level 90, Fully Grown").assertExists()
        compose.onNodeWithContentDescription("Peony, Level 99, Mastered").performClick()
        compose.onNodeWithText("Level 99 · Mastered").assertExists()
        compose.onNodeWithText("Water",useUnmergedTree=true).assertDoesNotExist()
    }
    @Test fun specimenWaterUsesCurrentLevelAndCentralPrice() {
        var requested:Int?=null
        compose.setContent { SkillzTheme { Surface { GreenScreen(GreenPreviewFixtures.mixed(GreenEnvironment.GARDEN),onWater={_,level,_->requested=level}) } } }
        compose.onNodeWithContentDescription("Daisy, Level 1, Sprout").performClick()
        compose.onNodeWithText("Water (Drops): 300").assertExists()
        compose.onNodeWithText("Next visible growth: Level 15").assertExists()
        compose.onNodeWithText("Water",useUnmergedTree=true).performClick()
        compose.runOnIdle { assertEquals(1,requested) }
    }
    @Test fun level99RetainsAdultFormWithMasterySequence() {
        val species = GreenCatalogue.byId.getValue("magnolia")
        val plant = GreenPreviewFixtures.specimen(species,99,1)
        val action = com.kingkharnivore.skillz.data.model.entity.green.GreenActionEntity("mastery","plant_watered",plant.id,plant.id,98,99,128400,1000,GreenPreviewFixtures.DATE)
        var closed = false
        compose.setContent { SkillzTheme { Surface { GreenScreen(GreenUiState(1000,listOf(plant), action=action),onDismissFeedback={closed=true}) } } }
        compose.onNodeWithText("Mastered").assertExists()
        compose.onNodeWithText("Level 99 · Fully Grown").assertExists()
        compose.onNodeWithText("Continue").performClick()
        compose.onAllNodesWithText("Planted:",substring=true).onLast().assertExists()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Magnolia Mastery ×1").assertExists()
        compose.onNodeWithText("Return to The Green").performClick()
        compose.runOnIdle { assertTrue(closed) }
    }
    @Test fun plantingOpensNewSpecimenAndWateringShowsExactlyOneLevelOfGrowth() {
        val state = androidx.compose.runtime.mutableStateOf(GreenUiState(drops = 10_000))
        compose.setContent {
            SkillzTheme {
                Surface {
                    GreenScreen(state.value, initialSection = GreenSection.CATALOGUE,
                        onPlant = { speciesId, token ->
                            val species = GreenCatalogue.byId.getValue(speciesId)
                            val plant = GreenPreviewFixtures.specimen(species, 1, 1)
                            val cost = GreenEconomy.seedCost(species.tier)
                            val balance = state.value.drops - cost
                            state.value = state.value.copy(drops = balance, specimens = listOf(plant),
                                action = com.kingkharnivore.skillz.data.model.entity.green.GreenActionEntity(
                                    token, "plant_seed_planted", speciesId, plant.id, 0, 1, cost, balance, GreenPreviewFixtures.DATE))
                        },
                        onWater = { specimenId, level, token ->
                            val plant = state.value.specimens.single()
                            assertEquals(plant.id, specimenId)
                            val cost = GreenEconomy.waterCost(GreenCatalogue.byId.getValue(plant.speciesId).tier, level)
                            val balance = state.value.drops - cost
                            state.value = state.value.copy(drops = balance, specimens = listOf(plant.copy(level = level + 1)),
                                action = com.kingkharnivore.skillz.data.model.entity.green.GreenActionEntity(
                                    token, "plant_watered", plant.id, plant.id, level, level + 1, cost, balance, GreenPreviewFixtures.DATE))
                        }, onDismissFeedback = { state.value = state.value.copy(action = null) })
                }
            }
        }
        compose.onNodeWithText("Daisy").performClick()
        compose.onNodeWithText("Plant Seed").performClick()
        compose.onAllNodesWithText("Level 1 · Sprout").onLast().assertIsDisplayed()
        compose.onNodeWithText("Water", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Level 2 · Sprout").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(7_900L, state.value.drops)
            assertEquals(1, state.value.specimens.size)
        }
    }

    @Test fun level90ShowsFullGrowthWhileWateringTowardMasteryRemainsAvailable() {
        val species = GreenCatalogue.byId.getValue("magnolia")
        val plant = GreenPreviewFixtures.specimen(species, 90, 1)
        val action = com.kingkharnivore.skillz.data.model.entity.green.GreenActionEntity(
            "full-growth", "plant_watered", plant.id, plant.id, 89, 90,
            GreenEconomy.waterCost(species.tier, 89), 250_000, GreenPreviewFixtures.DATE)
        compose.setContent {
            SkillzTheme {
                Surface {
                    GreenScreen(GreenUiState(250_000, listOf(plant), action = action), initialSpecimenId = plant.id)
                }
            }
        }
        compose.onAllNodesWithText("Level 90 · Fully Grown").onLast().assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Magnolia, Level 90, Fully Grown").onLast().assertIsDisplayed()
        compose.onNodeWithText("Water", useUnmergedTree = true).assertIsEnabled()
        compose.onNodeWithText("Next visible growth:", substring = true).assertDoesNotExist()
    }
    @Test fun collectionLivesInChestWithIndependentSpecimensAndWatering() {
        var level: Int? = null
        compose.setContent { SkillzTheme { Surface {
            GreenChestPreviewContent(GreenPreviewFixtures.mixed(GreenEnvironment.GARDEN),onWater={_,old,_->level=old})
        } } }
        compose.onNodeWithText("The Chest").assertIsDisplayed()
        compose.onNodeWithText("Plants").assertIsSelected()
        compose.onNodeWithText("Creatures").assertExists()
        compose.onNodeWithText("Daisy",substring=false).performClick()
        compose.onAllNodesWithText("Level 1 · Sprout").onLast().assertIsDisplayed()
        compose.onNodeWithText("Water",useUnmergedTree=true).performClick()
        compose.runOnIdle { assertEquals(1,level) }
    }
    @Test fun greenWorldLinksToSharedChestAndBadgesWithoutLocalCollectionTabs() {
        var chest=false;var badges=false
        compose.setContent { SkillzTheme { Surface {
            GreenScreen(GreenPreviewFixtures.mixed(GreenEnvironment.GARDEN),onOpenChest={chest=true},onOpenBadges={badges=true})
        } } }
        compose.onNodeWithText("Collection").assertDoesNotExist()
        compose.onNodeWithText("View Plants in The Chest").performClick()
        compose.onNodeWithText("View Green Badges").performClick()
        compose.runOnIdle { assertTrue(chest);assertTrue(badges) }
    }
    @Test fun singleSpeciesBadgeUsesItsNameAndSmallCollectionsUseTheirActualTarget() {
        compose.setContent { SkillzTheme { Surface {
            androidx.compose.foundation.layout.Column {
                listOf("green_v1_species_daisy_mastery", "green_v3_garden_15", "green_v2_first_seedling").forEach {
                    val presentation=com.kingkharnivore.skillz.ui.screen.shell.inventory.resolveBadgePresentation(it)
                    androidx.compose.material3.Text(presentation.description)
                }
            }
        } } }
        compose.onNodeWithText("Grow Daisy to Level 99.").assertExists()
        compose.onNodeWithText("Grow one plant to Level 15.").assertExists()
        compose.onNodeWithText("Grow 3 different species from this set to Level 15.",substring=true).assertExists()
        compose.onNodeWithText("Grow every species",substring=true).assertDoesNotExist()
    }

    @Test fun everyRealmPresentsEarlyGrowthWithAnExplicitRoomAndLevel() {
        compose.setContent { SkillzTheme { Surface {
            androidx.compose.foundation.layout.Column {
                listOf("sea", "land", "red").forEach {
                    val presentation=com.kingkharnivore.skillz.ui.screen.shell.inventory.resolveBadgePresentation("growth_v1_${it}_hello")
                    androidx.compose.material3.Text(presentation.description)
                }
            }
        } } }
        listOf("The Blue · Sea", "The Blue · Land", "The Red").forEach {
            compose.onNodeWithText("$it: Raise one creature to Level 5.").assertExists()
        }
    }

    @Test fun sharedBadgeBookShowsBotanicalObjectivesAndDetailActions() {
        var destination: com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination? = null
        compose.setContent { SkillzTheme { Surface {
            GreenBadgePreviewContent(GreenPreviewFixtures.mixed(GreenEnvironment.GARDEN), onNavigate = { destination = it })
        } } }
        compose.onAllNodesWithText("Badges").onFirst().assertIsDisplayed()
        compose.onNodeWithText("All badges").assertExists()
        compose.onNodeWithText("Category: The Green").performScrollTo().assertExists()
        val ancientCompany = hasContentDescription("Ancient Company · Flourish badge.", substring = true)
        compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(
            androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange
        )).performScrollToNode(ancientCompany)
        compose.onNode(ancientCompany).assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Locked").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Locked").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Grow every species in this set to Level 90.",substring=true).onLast().performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("View Plant").onLast().performScrollTo().performClick()
        compose.runOnIdle {
            val green = destination as com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination.Green
            assertTrue(green.plantSeed)
            assertTrue(green.speciesId in setOf("giant_sequoia", "redwood", "bristlecone_pine", "welwitschia"))
        }
    }

    @Test fun environmentsAreFullScreenVerticalDestinationsWithOverlayControls() {
        compose.setContent { SkillzTheme { Surface { GreenScreen(GreenUiState()) } } }
        compose.onNodeWithTag("green-environments").assert(
            SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange))
        GreenEnvironment.entries.forEachIndexed { index, environment ->
            compose.onNodeWithTag("green-environments").performScrollToIndex(index)
            compose.waitForIdle()
            compose.onNodeWithTag("green-specimens:${environment.id}").assertIsDisplayed()
            compose.onNodeWithText("World").assertIsSelected()
            compose.onNodeWithText("View Plants in The Chest").assertIsDisplayed()
            compose.onNodeWithText("View Green Badges").assertIsDisplayed()
        }
    }

    @Test fun verticalSwipesChangeEnvironmentWithoutScrollingThroughDensePlants() {
        val plants = GreenEnvironment.entries.flatMap { GreenPreviewFixtures.mixed(it).specimens }
        compose.setContent { SkillzTheme { Surface { GreenScreen(GreenUiState(specimens = plants)) } } }
        // Swipe directly over the plant plot, where a nested vertical list previously trapped gestures.
        compose.onNodeWithTag("green-specimens:garden").performTouchInput { swipeUp() }
        compose.onNodeWithTag("green-specimens:woodlands").assertIsDisplayed()
        compose.onNodeWithTag("green-specimens:woodlands").performTouchInput { swipeUp() }
        compose.onNodeWithTag("green-specimens:rainforest").assertIsDisplayed()
        compose.onNodeWithTag("green-specimens:rainforest").performTouchInput { swipeDown() }
        compose.onNodeWithTag("green-specimens:woodlands").assertIsDisplayed()
        compose.onNodeWithText("World").assertIsDisplayed()
        compose.onNode(hasText("View Green Badges") and hasAnyAncestor(hasTestTag("green-footer:woodlands"))).assertIsDisplayed()
    }

    @Test fun everyPlantPlotStaysOutsidePersistentTextAndControlsAtLargeFontSize() {
        val state = GreenPreviewFixtures.mixed(GreenEnvironment.GARDEN)
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, 1.3f)) {
                SkillzTheme { Surface { GreenScreen(state) } }
            }
        }
        listOf("Garden", "Woodlands", "Rainforest", "Wetlands", "Drylands", "Highlands").forEach { name ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNode(hasText(name) and hasAnyAncestor(hasTestTag("green-rail:garden")), useUnmergedTree = true)
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals("Environment label must fit at large text size: $name", 1, layouts.single().lineCount)
        }
        val visited = mutableSetOf<String>()
        do {
            val header = compose.onNodeWithTag("green-controls").fetchSemanticsNode().boundsInRoot
            val footer = compose.onNodeWithTag("green-footer:garden").fetchSemanticsNode().boundsInRoot
            val rail = compose.onNodeWithTag("green-rail:garden").fetchSemanticsNode().boundsInRoot
            val plot = compose.onNodeWithTag("green-specimens:garden").fetchSemanticsNode().boundsInRoot
            state.specimens.forEach { plant ->
                val nodes = compose.onAllNodesWithTag("green-plant:${plant.id}").fetchSemanticsNodes()
                nodes.filter { it.boundsInRoot.width > 0 && it.boundsInRoot.height > 0 }.forEach { node ->
                    val bounds = node.boundsInRoot
                    if (bounds.left >= plot.left && bounds.right <= plot.right) {
                        assertTrue("Plant under header: ${plant.id}", bounds.top >= header.bottom)
                        assertTrue("Plant under footer: ${plant.id}", bounds.bottom <= footer.top)
                        assertTrue("Plant under rail: ${plant.id}", bounds.right <= rail.left)
                        visited += plant.id
                    }
                }
            }
            compose.onNodeWithText("World").assertIsDisplayed()
            compose.onNodeWithText("View Green Badges").assertIsDisplayed()
            val next = compose.onNodeWithContentDescription("Next plot")
            val enabled = !next.fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
            if (!enabled) break
            next.performClick()
        } while (true)
        assertEquals(state.specimens.map { it.id }.toSet(), visited)
    }

    @Test fun badgeDeepLinkOpensAndWatersTheExactUnfinishedCopyInChest() {
        val species = GreenCatalogue.byId.getValue("daisy")
        val ready = GreenPreviewFixtures.specimen(species, 45, 1)
        val growing = GreenPreviewFixtures.specimen(species, 29, 2)
        var watered: String? = null
        compose.setContent { SkillzTheme { Surface {
            GreenCollectionScreen(GreenUiState(100000, listOf(ready, growing)),
                { id, _, _ -> watered=id }, {}, {}, "daisy", growing.id)
        } } }
        compose.onNodeWithText("Water").performClick()
        compose.runOnIdle { assertEquals(growing.id, watered) }
    }

    @Test fun seedErrorsStayVisibleInsideTheDialogAndUnownedPlantsHaveNoLevelZero() {
        val state = androidx.compose.runtime.mutableStateOf(GreenUiState(drops=10000))
        compose.setContent { SkillzTheme { Surface {
            GreenScreen(state.value, initialSection=GreenSection.CATALOGUE,
                onPlant={ _, _ -> state.value=state.value.copy(messageRes=com.kingkharnivore.skillz.R.string.green_error) })
        } } }
        compose.onAllNodesWithText("Not planted yet").onFirst().assertExists()
        compose.onNodeWithText("Highest level: 0", substring=true).assertDoesNotExist()
        compose.onNodeWithText("Daisy").performClick()
        compose.onNodeWithText("Plant Seed").performClick()
        compose.onNodeWithText("Growth could not be saved. Please try again.").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(6000)
        compose.onNodeWithText("Growth could not be saved. Please try again.").assertIsDisplayed()
    }

    @Test fun chestWateringErrorsAppearInsideTheSpecimenDialog() {
        val plant=GreenPreviewFixtures.specimen(GreenCatalogue.byId.getValue("daisy"), 1, 1)
        val state=androidx.compose.runtime.mutableStateOf(GreenUiState(10000, listOf(plant)))
        compose.setContent { SkillzTheme { Surface {
            GreenCollectionScreen(state.value,
                { _, _, _ -> state.value=state.value.copy(messageRes=com.kingkharnivore.skillz.R.string.green_error) },
                {}, {}, "daisy", plant.id)
        } } }
        compose.onNodeWithText("Water (Drops): 300").performScrollTo()
        compose.onNodeWithText("Water").performClick()
        compose.onNodeWithText("Growth could not be saved. Please try again.").assertIsDisplayed()
    }

    @Test fun masteryReturnsToTheEnvironmentWithoutLeavingSpecimenDialogOpen() {
        val plant=GreenPreviewFixtures.specimen(GreenCatalogue.byId.getValue("magnolia"),99,1)
        val action=com.kingkharnivore.skillz.data.model.entity.green.GreenActionEntity(
            "return-mastery","plant_watered",plant.id,plant.id,98,99,1,1000,GreenPreviewFixtures.DATE)
        val state=androidx.compose.runtime.mutableStateOf(GreenUiState(1000,listOf(plant),action=action))
        compose.setContent { SkillzTheme { Surface {
            GreenScreen(state.value, initialSection=GreenSection.CATALOGUE, initialSpecimenId=plant.id,
                onDismissFeedback={ state.value=state.value.copy(action=null) })
        } } }
        compose.onNodeWithText("Close").assertDoesNotExist()
        repeat(2) { compose.onNodeWithText("Continue").performClick() }
        compose.onNode(hasContentDescription("Magnolia · Mastery", substring=true)).assertExists()
        compose.onNodeWithText("Return to The Green").performClick()
        compose.onNodeWithTag("green-environments").assertExists()
        compose.onNodeWithText("Close").assertDoesNotExist()
        compose.onNodeWithText("Continue").assertDoesNotExist()
    }

    @Test fun masteryInChestReturnsToCollectionWithoutLeavingSpecimenDialogOpen() {
        val plant=GreenPreviewFixtures.specimen(GreenCatalogue.byId.getValue("daisy"),98,1)
        val state=androidx.compose.runtime.mutableStateOf(GreenUiState(100000,listOf(plant)))
        compose.setContent { SkillzTheme { Surface {
            GreenCollectionScreen(state.value, { _, _, token ->
                state.value=state.value.copy(specimens=listOf(plant.copy(level=99)),
                    action=com.kingkharnivore.skillz.data.model.entity.green.GreenActionEntity(
                        token,"plant_watered",plant.id,plant.id,98,99,1,99999,GreenPreviewFixtures.DATE))
            }, { state.value=state.value.copy(action=null) }, {}, "daisy", plant.id)
        } } }
        compose.onNodeWithText("Water").performClick()
        compose.onNodeWithText("Close").assertDoesNotExist()
        repeat(2) { compose.onNodeWithText("Continue").performClick() }
        compose.onNodeWithText("Return to The Chest").performClick()
        compose.onNodeWithText("Close").assertDoesNotExist()
        compose.onNodeWithText("Level 99 · Mastered").assertExists()
    }

    @Test fun deepLinkedSeedAndEnvironmentCatalogueVisitsAreRecordedOncePerOpen() {
        val events=mutableListOf<Triple<String,GreenEnvironment?,String?>>()
        compose.setContent { SkillzTheme { Surface {
            GreenScreen(GreenUiState(), initialSection=GreenSection.CATALOGUE, initialSeedId="daisy",
                onView={ name, env, species -> events.add(Triple(name,env,species)) })
        } } }
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Woodlands").performClick()
        compose.runOnIdle {
            assertEquals(1,events.count { it.first=="plant_seed_viewed" && it.third=="daisy" })
            assertEquals(1,events.count { it.first=="green_catalogue_opened" && it.second==GreenEnvironment.GARDEN })
            assertEquals(1,events.count { it.first=="green_catalogue_opened" && it.second==GreenEnvironment.WOODLANDS })
        }
    }

}
