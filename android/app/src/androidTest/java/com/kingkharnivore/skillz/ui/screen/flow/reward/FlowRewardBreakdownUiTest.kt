package com.kingkharnivore.skillz.ui.screen.flow.reward

import android.graphics.Bitmap
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

class FlowRewardBreakdownUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun surgeRowShowsBonusWhileTotalRemainsFour() {
        compose.setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.width(360.dp).padding(16.dp)) {
                    SessionRewardContent(
                        r = FlowRewardUiModel(
                            minutes = 3,
                            baseScyraPoints = 3,
                            tenMinuteBonuses = 0,
                            thirtyMinuteBonuses = 0,
                            sixtyMinuteBonuses = 0,
                            finalScyraPoints = 4,
                            surgePoints = 4
                        ),
                        calmMode = false
                    )
                }
            }
        }
        compose.onNodeWithText("4 Scyra Points").assertIsDisplayed()
        compose.onNodeWithText("Base Flow 3\nSurge +1", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Surge +4", substring = true).assertDoesNotExist()
        val folder = InstrumentationRegistry.getInstrumentation().targetContext
            .getExternalFilesDir("flow-rewards")!!.apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(folder, "surge-bonus-breakdown.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
