package com.kingkharnivore.skillz.domain.power

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.ui.screen.shell.inventory.*
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.utils.shell.*
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ChestCollectionUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext.let {
        it.createConfigurationContext(Configuration(it.resources.configuration).apply { setLocales(LocaleList.forLanguageTags("en")) })
    }
    private fun find(id: String, level: Int = 1) = UserShellFindInstanceEntity("$id-$level",id,1,"test",null,null,null,false,true,animalLevel=level)
    @Composable private fun Theme(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration) {
            SkillzTheme(dynamicColor=false) { content() }
        }
    }
    @Test fun environmentSelectionPreservesProgressAndClearResetsBoth() {
        val state=mutableStateOf(ShellUiState(finds=listOf(find("triassic_saturnalia"),find("creature_chicken",99)),chestFilter=ChestFilterOption.NotMastered))
        compose.setContent { Theme { ShellChestScreen(state.value,{_,_->},{_,_->},{},{},{state.value=state.value.copy(chestFilter=it)},
            onEnvironmentSelected={state.value=state.value.copy(chestEnvironment=it)},
            onClearFilters={state.value=state.value.copy(chestEnvironment=ChestFilterOption.All,chestFilter=ChestFilterOption.All)}) } }
        compose.onNodeWithTag("chest-environment").performClick()
        compose.onNodeWithTag("chest-environment-land").performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.chest_filter_clear)).performClick()
        compose.runOnIdle { assertEquals(ChestFilterOption.All,state.value.chestFilter); assertEquals(ChestFilterOption.All,state.value.chestEnvironment) }
        compose.onNodeWithContentDescription("Saturnalia",substring=true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Chicken",substring=true).assertIsDisplayed()
    }
    @Test fun focusedSpeciesOutsideFilterHasCorrectSourceAndStaleActionsDisappear() {
        val state=mutableStateOf(ShellUiState(finds=listOf(find("jurassic_stegosaurus")),chestEnvironment=ChestFilterOption.Land))
        compose.setContent { Theme { ShellChestScreen(state.value,{_,_->},{_,_->},{},{},{},focusSpeciesId="jurassic_stegosaurus") } }
        compose.onNodeWithText("The Red · Jurassic").assertIsDisplayed()
        compose.runOnIdle { state.value=state.value.copy(finds=emptyList()) }
        compose.onNodeWithText("The Red · Jurassic").assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.chest_open_red)).assertIsDisplayed()
    }
    @Test fun detailReleaseAndConfirmationRemainReachableAtLargeFont() {
        var released=false
        compose.setContent { Theme { CompositionLocalProvider(LocalDensity provides Density(context.resources.displayMetrics.density,1.5f)) {
            ShellChestScreen(ShellUiState(finds=listOf(find("jurassic_stegosaurus",98)),pearlBalance=200_000),{_,_->released=true},{_,_->},{},{},{},focusSpeciesId="jurassic_stegosaurus")
        } } }
        compose.onNodeWithContentDescription(context.getString(R.string.shell_chest_release_button_a11y)).performScrollTo().performClick()
        compose.onNodeWithTag("chest-confirm-release").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(released) }
    }
    @Test fun emptyChestNavigatesToBothBlueAndRed() {
        var destination=""
        compose.setContent { Theme { ShellChestScreen(ShellUiState(),{_,_->},{_,_->},{destination="blue"},{},{},onOpenRed={destination="red"}) } }
        compose.onNodeWithText(context.getString(R.string.shell_chest_empty_action)).performClick()
        compose.runOnIdle { assertEquals("blue",destination) }
        compose.onNodeWithText(context.getString(R.string.chest_open_red)).performClick()
        compose.runOnIdle { assertEquals("red",destination) }
    }
    @Test fun bookCollectionsOpenMembersAndRetainAllBadgesBrowsing() {
        val dashboard=BadgeDashboardCalculator.calculate(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList())
        compose.setContent { Theme { BadgesScreen(ShellUiState(badgeDashboard=dashboard),{_,_->},{},{},{},{},{},{},{},{},{},{},{},initialTab=BadgesTab.BADGE_BOOK) } }
        compose.onNodeWithTag("badge_book_first_steps").performScrollTo().performClick()
        compose.onNodeWithTag("book-member-badge_flow_10_min").assertIsDisplayed().performClick()
        compose.onNodeWithText(context.getString(R.string.shell_badge_flow_10_title)).assertIsDisplayed()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithText(context.getString(R.string.book_all_badges)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.badges_search)).performScrollTo().assertIsDisplayed()
    }
}
