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

/**
 * Immutable UI state for the task create/edit form.
 *
 * @property title current title field value.
 * @property description current description field value.
 * @property status selected status (`"TODO"`, `"IN_PROGRESS"`, `"DONE"`).
 * @property priority selected priority (`"LOW"`, `"MEDIUM"`, `"HIGH"`).
 * @property dueDate selected due date in epoch milliseconds, or `null` if none.
 * @property isLoading whether a save operation is in progress.
 * @property isSaved whether the task has been saved (used to trigger navigation back).
 * @property errorMessage validation error to show, or `null`.
 * @property isEditing `true` when editing an existing task, `false` when creating a new one.
 */
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

/**
 * ViewModel backing the task create/edit screen.
 *
 * Reads `projectId` and `taskId` from [SavedStateHandle] (navigation arguments). When a valid
 * `taskId` is present the existing task is loaded into the form for editing; otherwise the form
 * starts empty for creation. Exposes form state as a [StateFlow] and persists via [TaskRepository].
 *
 * @property taskRepository task persistence and queries.
 */
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

    /**
     * Updates the title field as the user types.
     *
     * @param title new title value.
     */
    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title, errorMessage = null)
    }

    /**
     * Updates the description field as the user types.
     *
     * @param description new description value.
     */
    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    /**
     * Updates the selected status.
     *
     * @param status new status value.
     */
    fun onStatusChange(status: String) {
        _uiState.value = _uiState.value.copy(status = status)
    }

    /**
     * Updates the selected priority.
     *
     * @param priority new priority value.
     */
    fun onPriorityChange(priority: String) {
        _uiState.value = _uiState.value.copy(priority = priority)
    }

    /**
     * Updates the selected due date.
     *
     * @param dueDate new due date in epoch milliseconds, or `null` to clear it.
     */
    fun onDueDateChange(dueDate: Long?) {
        _uiState.value = _uiState.value.copy(dueDate = dueDate)
    }

    /**
     * Validates the form and persists the task, updating the existing task when editing or creating
     * a new one otherwise. Sets an error and returns early if the title is blank. On success sets
     * `isSaved` so the screen can navigate back. When editing, `isCompleted` is derived from a
     * status of `"DONE"`.
     */
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