package com.kingkharnivore.skillz.viewmodel

import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ArcMetadata
import org.junit.Assert.*
import org.junit.Test

class ArcEditorUiStateTest {
    @Test fun loadingAndLoadFailureAreNotEditableOrDirty() {
        assertFalse(ArcEditorUiState(arcId = 1, isLoading = true).canSave)
        val failed = ArcEditorUiState(arcId = 1, loadErrorResId = R.string.arc_details_load_error)
        assertFalse(failed.isDirty)
        assertFalse(failed.canSave)
    }

    @Test fun populatedAndEmptyLoadsUseNormalizedBaseline() {
        val empty = ArcMetadata(1)
        assertFalse(ArcEditorUiState(arcId = 1, baseline = empty).isDirty)
        val populated = ArcMetadata(1, title = "Morning Flow")
        val state = ArcEditorUiState(arcId = 1, title = "  Morning Flow  ", baseline = populated)
        assertFalse(state.isDirty)
        assertFalse(state.canSave)
    }

    @Test fun changedLoadedContentCanSaveButSavingCannot() {
        val baseline = ArcMetadata(1)
        val changed = ArcEditorUiState(arcId = 1, title = "New", baseline = baseline)
        assertTrue(changed.isDirty)
        assertTrue(changed.canSave)
        assertFalse(changed.copy(isSaving = true).canSave)
    }
}
