package com.example.ai_tranning.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_tranning.data.local.entity.ProjectEntity
import com.example.ai_tranning.data.local.entity.TaskEntity
import com.example.ai_tranning.data.repository.ProjectRepository
import com.example.ai_tranning.data.repository.TaskRepository
import com.example.ai_tranning.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable UI state for the project-details screen.
 *
 * @property project the project being viewed, or `null` until loaded.
 * @property tasks the project's tasks, optionally filtered by status or priority.
 * @property isLoading whether the initial task load is still in progress.
 * @property statusFilter active status filter, or `null` when not filtering by status.
 * @property priorityFilter active priority filter, or `null` when not filtering by priority.
 */
data class ProjectUiState(
    val project: ProjectEntity? = null,
    val tasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = true,
    val statusFilter: String? = null,
    val priorityFilter: String? = null
)

/**
 * ViewModel backing the project-details screen.
 *
 * Reads the `projectId` from [SavedStateHandle] (navigation argument), loads the project and its
 * tasks into a [StateFlow], and supports mutually-exclusive status/priority filtering, toggling task
 * completion, and task deletion. Persistence is delegated to [ProjectRepository] and
 * [TaskRepository].
 *
 * @property projectRepository project lookup.
 * @property taskRepository task queries, filtering, and mutations.
 * @property sessionManager supplies the currently logged-in user id for ownership checks.
 */
@HiltViewModel
class ProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>("projectId") ?: -1

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState

    init {
        loadProject()
        loadTasks()
    }

    private fun loadProject() {
        viewModelScope.launch {
            val userId = sessionManager.loggedInUserId.first() ?: return@launch
            // Ownership-checked: returns null for a project the logged-in user does not own,
            // so another user's project is never shown.
            val project = projectRepository.getProject(userId, projectId)
            _uiState.value = _uiState.value.copy(project = project)
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            taskRepository.getTasks(projectId).collect { tasks ->
                _uiState.value = _uiState.value.copy(tasks = tasks, isLoading = false)
            }
        }
    }

    /**
     * Filters the task list by workflow status; passing `null` clears the filter and reloads all
     * tasks. Clears any active priority filter.
     *
     * @param status status to filter by (`"TODO"`, `"IN_PROGRESS"`, `"DONE"`), or `null` for all.
     */
    fun filterByStatus(status: String?) {
        _uiState.value = _uiState.value.copy(statusFilter = status, priorityFilter = null)
        viewModelScope.launch {
            if (status == null) {
                loadTasks()
            } else {
                taskRepository.filterByStatus(projectId, status).collect { tasks ->
                    _uiState.value = _uiState.value.copy(tasks = tasks)
                }
            }
        }
    }

    /**
     * Filters the task list by priority; passing `null` clears the filter and reloads all tasks.
     * Clears any active status filter.
     *
     * @param priority priority to filter by (`"LOW"`, `"MEDIUM"`, `"HIGH"`), or `null` for all.
     */
    fun filterByPriority(priority: String?) {
        _uiState.value = _uiState.value.copy(priorityFilter = priority, statusFilter = null)
        viewModelScope.launch {
            if (priority == null) {
                loadTasks()
            } else {
                taskRepository.filterByPriority(projectId, priority).collect { tasks ->
                    _uiState.value = _uiState.value.copy(tasks = tasks)
                }
            }
        }
    }

    /**
     * Toggles a task's completion state, flipping `isCompleted` and setting `status` to `"DONE"`
     * when completed or `"TODO"` when reopened.
     *
     * @param task the task to toggle.
     */
    fun toggleTaskStatus(task: TaskEntity) {
        viewModelScope.launch {
            val newStatus = if (task.isCompleted) "TODO" else "DONE"
            taskRepository.updateTask(
                task.copy(isCompleted = !task.isCompleted, status = newStatus)
            )
        }
    }

    /**
     * Deletes a task from the project.
     *
     * @param taskId id of the task to delete.
     */
    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }
}