package com.example.ai_tranning.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_tranning.data.local.entity.ProjectEntity
import com.example.ai_tranning.data.local.entity.TaskEntity
import com.example.ai_tranning.data.repository.ProjectRepository
import com.example.ai_tranning.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectUiState(
    val project: ProjectEntity? = null,
    val tasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = true,
    val statusFilter: String? = null,
    val priorityFilter: String? = null
)

@HiltViewModel
class ProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository
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
            val project = projectRepository.getProject(projectId)
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

    fun toggleTaskStatus(task: TaskEntity) {
        viewModelScope.launch {
            val newStatus = if (task.isCompleted) "TODO" else "DONE"
            taskRepository.updateTask(
                task.copy(isCompleted = !task.isCompleted, status = newStatus)
            )
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }
}