package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import android.app.LocaleManager
import android.graphics.Bitmap
import android.os.LocaleList
import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.shell.*
import com.kingkharnivore.skillz.ui.screen.shell.inventory.creatureCompletionText
import com.kingkharnivore.skillz.ui.screen.shell.inventory.masterySupportText
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.utils.shell.CreatureEconomy
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Real per-app locales reach Android dialog windows as well as the parent Compose page. */
@SdkSuppress(minSdkVersion = 33)
@RunWith(Parameterized::class)
class CreatureLocalizationUiTest(private val language: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun languages() = listOf(arrayOf("en"), arrayOf("hi"), arrayOf("mr"), arrayOf("es"))
    }
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule(order = 0) val localeRule = object : ExternalResource() {
        private lateinit var original: LocaleList
        override fun before() {
            val manager = context.getSystemService(LocaleManager::class.java)
            original = manager.applicationLocales
            try {
                manager.applicationLocales = LocaleList.forLanguageTags(language)
                val deadline = SystemClock.uptimeMillis() + 5000
                while (context.resources.configuration.locales[0].language != language && SystemClock.uptimeMillis() < deadline) {
                    SystemClock.sleep(50)
                }
                check(context.resources.configuration.locales[0].language == language) { "App locale did not change to $language" }
            } catch (error: Throwable) {
                // ExternalResource does not invoke after() when before() fails.
                manager.applicationLocales = original
                throw error
            }
        }
        override fun after() {
            context.getSystemService(LocaleManager::class.java).applicationLocales = original
        }
    }
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun multipleCopyReleaseHasLocalizedTalkBackDescriptions() = checkRelease(3)
    @Test fun singleCopyReleaseHasLocalizedTalkBackDescription() = checkRelease(1)

    private fun checkRelease(count: Int) {
        val animal = TheBlueAnimalGroupUiModel("creature_tiger", TheBlueZoneId.GREAT_WILD,
            count, 0, count, null, listOf(FormCountUiModel("Level 1", count)), null, false)
        compose.setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                ReleaseCreatureConfirmationSheet(animal, {}, { _, _ -> })
            }
        }
        val name = context.getString(R.string.land_creature_tiger)
        compose.onNodeWithText(context.getString(R.string.shell_creature_release_confirm_title, name)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.creature_release_reward_a11y,
            CreatureEconomy.releaseValuePearls("creature_tiger", 1)), useUnmergedTree = true).assertExists()
        if (count > 1) {
            compose.onNodeWithContentDescription(context.getString(R.string.creature_release_selected_a11y,
                1, count, name), useUnmergedTree = true).assertExists()
            compose.onNodeWithContentDescription(context.getString(R.string.creature_release_level_a11y,
                1, name, count, 1), useUnmergedTree = true).assertExists()
            // Expand the sheet using the same accessibility action exposed to TalkBack.
            compose.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.Expand),
                useUnmergedTree = true)[0].performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.Expand) { it() }
        }
        screenshot("$language-release-$count")
    }

    @Test fun realmCompletionAndMasteryUseLocalizedSpeciesWithoutEnglishPluralization() {
        compose.setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                Column {
                    Text(creatureCompletionText("creature_tiger"))
                    Text(creatureCompletionText("creature_tiger", includesHabitats = true))
                    Text(creatureCompletionText("focus_minnow"))
                    Text(creatureCompletionText("focus_minnow", includesHabitats = true))
                    Text(masterySupportText(1, context.getString(R.string.land_creature_chicken)))
                    Text(masterySupportText(2, context.getString(R.string.land_creature_sheep)))
                    Text(masterySupportText(3, context.getString(R.string.land_creature_deer)))
                    Text(masterySupportText(4, context.getString(R.string.land_creature_moose)))
                }
            }
        }
        listOf(R.string.collection_land, R.string.collection_all_land, R.string.collection_the_blue,
            R.string.collection_all_waters).forEach { collection ->
            compose.onNodeWithText(context.getString(R.string.level99_preview_completes_realm,
                context.getString(collection))).assertIsDisplayed()
        }
        compose.onNodeWithText(context.getString(R.string.mastery_second_species_support,
            context.getString(R.string.land_creature_sheep))).assertIsDisplayed()
        screenshot("$language-mastery-copy")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        // UiAutomation captures SurfaceFlinger, which can trail the Compose test clock.
        SystemClock.sleep(750)
        val folder = context.getExternalFilesDir("localization-fixes")!!.apply { mkdirs() }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        var screenshot = automation.takeScreenshot()
        repeat(3) {
            if (screenshot == null) {
                SystemClock.sleep(250)
                screenshot = automation.takeScreenshot()
            }
        }
        val bitmap = checkNotNull(screenshot) { "Unable to capture $name" }
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
