package com.example.ai_tranning.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_tranning.data.local.entity.TaskEntity
import com.example.ai_tranning.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskUiState(
    val title: String = "",
    val description: String = "",
    val status: String = "TODO",
    val priority: String = "MEDIUM",
    val dueDate: Long? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val isEditing: Boolean = false
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>("projectId") ?: -1
    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: -1

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState

    init {
        if (taskId > 0) {
            loadTask()
        }
    }

    private fun loadTask() {
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId)
            if (task != null) {
                _uiState.value = _uiState.value.copy(
                    title = task.title,
                    description = task.description,
                    status = task.status,
                    priority = task.priority,
                    dueDate = task.dueDate,
                    isEditing = true
                )
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title, errorMessage = null)
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun onStatusChange(status: String) {
        _uiState.value = _uiState.value.copy(status = status)
    }

    fun onPriorityChange(priority: String) {
        _uiState.value = _uiState.value.copy(priority = priority)
    }

    fun onDueDateChange(dueDate: Long?) {
        _uiState.value = _uiState.value.copy(dueDate = dueDate)
    }

    fun saveTask() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Task title is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            if (state.isEditing) {
                val existing = taskRepository.getTask(taskId)
                if (existing != null) {
                    taskRepository.updateTask(
                        existing.copy(
                            title = state.title,
                            description = state.description,
                            status = state.status,
                            priority = state.priority,
                            dueDate = state.dueDate,
                            isCompleted = state.status == "DONE"
                        )
                    )
                }
            } else {
                taskRepository.createTask(
                    projectId = projectId,
                    title = state.title,
                    description = state.description,
                    priority = state.priority,
                    dueDate = state.dueDate
                )
            }
            _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true)
        }
    }
}