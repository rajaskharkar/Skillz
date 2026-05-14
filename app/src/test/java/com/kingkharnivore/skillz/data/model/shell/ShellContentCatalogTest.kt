package com.kingkharnivore.skillz.data.model.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellContentCatalogTest {
    @Test
    fun focusPearlObjects_haveStableV1CostsAndCompatibleSlots() {
        val objects = ShellContentCatalog.focusPearlObjects

        assertEquals(5, objects.size)
        assertEquals(80, objects.first { it.findId == ShellContentCatalog.FOCUS_MOON_CORAL_LIGHT }.pearlCost)
        assertEquals(120, objects.first { it.findId == ShellContentCatalog.FOCUS_SEAHORSE_PERCH }.pearlCost)
        assertEquals(60, objects.first { it.findId == ShellContentCatalog.FOCUS_REEF_PEBBLE_BED }.pearlCost)
        assertTrue(objects.all { it.isPearlObject && it.placeable })
        assertTrue(objects.all { it.acceptedSlotTypes.isNotEmpty() })
    }

    @Test
    fun focusPearlObjects_areIndividualCopiesNotStacks() {
        assertTrue(ShellContentCatalog.focusPearlObjects.all { it.isPearlObject })
        assertFalse(ShellContentCatalog.focusPearlObjects.any { it.stackable })
    }

    @Test
    fun focusSlots_useLocalizedTitles() {
        assertTrue(ShellContentCatalog.focusSlots.all { it.titleRes != 0 })
    }

    @Test
    fun glowShellForms_arePerCopyForms() {
        val forms = ShellContentCatalog.upgradesFor(ShellContentCatalog.FOCUS_GLOW_SHELL)

        assertEquals(3, forms.size)
        assertEquals("focus_glow_shell_form_1", forms[0].upgradeStageId)
        assertEquals("focus_glow_shell_form_2", forms[1].upgradeStageId)
        assertEquals("focus_glow_shell_form_3", forms[2].upgradeStageId)
    }
}
