package com.kingkharnivore.skillz.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.SkillListUiState
import com.kingkharnivore.skillz.data.repository.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillListViewModel @Inject constructor(
    private val skillRepository: SkillRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SkillListUiState())
    val uiState: StateFlow<SkillListUiState> = _uiState.asStateFlow()

    init {
        observeSkills()
    }

    private fun observeSkills() {
        viewModelScope.launch {
            skillRepository.getAllSkills()
                .onStart {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                .collect { skills ->
                    _uiState.value = SkillListUiState(
                        isLoading = false,
                        skills = skills,
                        errorMessage = null
                    )
                }
        }
    }

    fun addSkill(name: String, description: String) {
        viewModelScope.launch {
            try {
                skillRepository.addSkill(name, description)
                // Flow will emit the updated list automatically
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to add skill"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}