package com.example.ai_tranning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_tranning.data.local.entity.ProjectEntity
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
 * A project paired with its task progress counts, as shown on a dashboard card.
 *
 * @property project the project entity.
 * @property taskCount total number of tasks in the project.
 * @property completedCount number of completed tasks in the project.
 */
data class ProjectWithCount(
    val project: ProjectEntity,
    val taskCount: Int = 0,
    val completedCount: Int = 0
)

/**
 * Immutable UI state for the dashboard screen.
 *
 * @property projects the user's projects with their progress counts.
 * @property isLoading whether the initial project load is still in progress.
 * @property showCreateDialog whether the create/edit project dialog is visible.
 * @property editingProject the project being edited, or `null` when creating a new one.
 * @property projectName current value of the name field in the dialog.
 * @property projectDescription current value of the description field in the dialog.
 * @property errorMessage validation error to show in the dialog, or `null`.
 */
data class DashboardUiState(
    val projects: List<ProjectWithCount> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val editingProject: ProjectEntity? = null,
    val projectName: String = "",
    val projectDescription: String = "",
    val errorMessage: String? = null
)

/**
 * ViewModel backing the dashboard screen.
 *
 * Resolves the logged-in user via [SessionManager], then observes that user's projects (enriched
 * with per-project task counts) as a [StateFlow]. Also drives the create/edit project dialog and
 * project deletion, delegating persistence to [ProjectRepository] and [TaskRepository].
 *
 * @property projectRepository project persistence and queries.
 * @property taskRepository task counts used to compute per-project progress.
 * @property sessionManager supplies the currently logged-in user id and clears the session on logout.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    private var userId: Long = -1

    init {
        viewModelScope.launch {
            userId = sessionManager.loggedInUserId.first() ?: return@launch
            loadProjects()
        }
    }

    private fun loadProjects() {
        viewModelScope.launch {
            projectRepository.getProjects(userId).collect { projects ->
                val projectsWithCounts = projects.map { project ->
                    val taskCount = taskRepository.getTaskCount(project.id).first()
                    val completedCount = taskRepository.getCompletedTaskCount(project.id).first()
                    ProjectWithCount(project, taskCount, completedCount)
                }
                _uiState.value = _uiState.value.copy(
                    projects = projectsWithCounts,
                    isLoading = false
                )
            }
        }
    }

    /** Opens the dialog in "create" mode with empty fields. */
    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            editingProject = null,
            projectName = "",
            projectDescription = ""
        )
    }

    /**
     * Opens the dialog in "edit" mode, pre-filled with the given project's values.
     *
     * @param project the project to edit.
     */
    fun showEditDialog(project: ProjectEntity) {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            editingProject = project,
            projectName = project.name,
            projectDescription = project.description
        )
    }

    /** Closes the dialog and resets its fields and any error. */
    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = false,
            editingProject = null,
            projectName = "",
            projectDescription = "",
            errorMessage = null
        )
    }

    /**
     * Updates the project-name field as the user types.
     *
     * @param name new name value.
     */
    fun onProjectNameChange(name: String) {
        _uiState.value = _uiState.value.copy(projectName = name, errorMessage = null)
    }

    /**
     * Updates the project-description field as the user types.
     *
     * @param description new description value.
     */
    fun onProjectDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(projectDescription = description)
    }

    /**
     * Validates the dialog and persists the project, creating a new one or updating the project
     * currently being edited. Sets an error message and returns early if the name is blank;
     * otherwise dismisses the dialog on success.
     */
    fun saveProject() {
        val state = _uiState.value
        if (state.projectName.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Project name is required")
            return
        }

        viewModelScope.launch {
            val editing = state.editingProject
            if (editing != null) {
                projectRepository.updateProject(
                    userId = userId,
                    id = editing.id,
                    name = state.projectName,
                    description = state.projectDescription
                )
            } else {
                projectRepository.createProject(userId, state.projectName, state.projectDescription)
            }
            dismissDialog()
        }
    }

    /**
     * Deletes a project (its tasks are removed via cascade delete).
     *
     * @param projectId id of the project to delete.
     */
    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            projectRepository.deleteProject(userId, projectId)
        }
    }

    /**
     * Clears the session and signals that logout has completed.
     *
     * @param onLoggedOut callback invoked after the session is cleared (e.g. to navigate to login).
     */
    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            sessionManager.clearSession()
            onLoggedOut()
        }
    }
}