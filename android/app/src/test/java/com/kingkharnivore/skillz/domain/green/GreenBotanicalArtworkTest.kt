package com.kingkharnivore.skillz.domain.green

import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.BotanicalForm
import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.BotanicalIllustrations
import org.junit.Assert.*
import org.junit.Test

class GreenBotanicalArtworkTest {
    @Test fun everySpeciesHasExplicitBotanicalArtDirectionAndAdultAssetsStayStable() {
        assertEquals(GreenCatalogue.byId.keys, BotanicalIllustrations.species.keys)
        GreenCatalogue.species.forEach { species ->
            assertEquals(7, species.stageAssetKeys.distinct().size)
            assertEquals(species.assetKey(90), species.assetKey(99))
        }
        assertEquals(BotanicalForm.CUP, BotanicalIllustrations.species.getValue("tulip").form)
        assertEquals(BotanicalForm.SUNFLOWER, BotanicalIllustrations.species.getValue("sunflower").form)
        assertEquals(BotanicalForm.TITAN, BotanicalIllustrations.species.getValue("corpse_flower").form)
        assertEquals(BotanicalForm.RAFFLESIA, BotanicalIllustrations.species.getValue("rafflesia").form)
        assertEquals(BotanicalForm.WELWITSCHIA, BotanicalIllustrations.species.getValue("welwitschia").form)
        assertNotEquals(BotanicalIllustrations.species.getValue("daisy").flower, BotanicalIllustrations.species.getValue("rose").flower)
    }
}
