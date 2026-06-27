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

data class ProjectWithCount(
    val project: ProjectEntity,
    val taskCount: Int = 0,
    val completedCount: Int = 0
)

data class DashboardUiState(
    val projects: List<ProjectWithCount> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val editingProject: ProjectEntity? = null,
    val projectName: String = "",
    val projectDescription: String = "",
    val errorMessage: String? = null
)

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

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            editingProject = null,
            projectName = "",
            projectDescription = ""
        )
    }

    fun showEditDialog(project: ProjectEntity) {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            editingProject = project,
            projectName = project.name,
            projectDescription = project.description
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = false,
            editingProject = null,
            projectName = "",
            projectDescription = "",
            errorMessage = null
        )
    }

    fun onProjectNameChange(name: String) {
        _uiState.value = _uiState.value.copy(projectName = name, errorMessage = null)
    }

    fun onProjectDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(projectDescription = description)
    }

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
                    editing.copy(name = state.projectName, description = state.projectDescription)
                )
            } else {
                projectRepository.createProject(userId, state.projectName, state.projectDescription)
            }
            dismissDialog()
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            sessionManager.clearSession()
            onLoggedOut()
        }
    }
}