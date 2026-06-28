package com.example.ai_tranning.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.ai_tranning.data.local.entity.ProjectEntity
import com.example.ai_tranning.data.local.entity.TaskEntity
import com.example.ai_tranning.data.repository.ProjectRepository
import com.example.ai_tranning.data.repository.TaskRepository
import com.example.ai_tranning.util.MainDispatcherRule
import com.example.ai_tranning.utils.SessionManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [ProjectViewModel], with a focus on task filtering by status and priority.
 *
 * Collaborators are mocked; the [MainDispatcherRule]'s unconfined dispatcher makes the project/task
 * loads launched in `init` (and the filter collections) run synchronously.
 *
 * Coverage:
 *  - The project is loaded with ownership enforcement (`getProject(userId, projectId)`).
 *  - All tasks load by default.
 *  - Filtering by each status (TODO / IN_PROGRESS / DONE) updates the list and the active filter.
 *  - Clearing the status filter reloads all tasks.
 *  - Filtering by priority replaces an active status filter.
 *  - Toggling completion and deleting a task delegate to the repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = 7L
    private val projectId = 1L
    private lateinit var projectRepository: ProjectRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager

    private val project = ProjectEntity(id = projectId, userId = userId, name = "P1", description = "d")
    private val todo = TaskEntity(id = 1, projectId = projectId, title = "todo", status = "TODO")
    private val inProgress =
        TaskEntity(id = 2, projectId = projectId, title = "wip", status = "IN_PROGRESS")
    private val done =
        TaskEntity(id = 3, projectId = projectId, title = "done", status = "DONE", isCompleted = true)
    private val allTasks = listOf(todo, inProgress, done)

    @Before
    fun setUp() {
        projectRepository = mockk(relaxed = true)
        taskRepository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        every { sessionManager.loggedInUserId } returns flowOf(userId)
        every { taskRepository.getTasks(projectId) } returns flowOf(allTasks)
    }

    private fun build(): ProjectViewModel {
        val handle = SavedStateHandle(mapOf("projectId" to projectId))
        return ProjectViewModel(handle, projectRepository, taskRepository, sessionManager)
    }

    @Test
    fun `loads the project with ownership and all of its tasks`() = runTest {
        coVerifyOwnershipReturns(project)

        val viewModel = build()

        val state = viewModel.uiState.value
        assertEquals(project, state.project)
        assertEquals(allTasks, state.tasks)
        assertFalse(state.isLoading)
        assertNull(state.statusFilter)
        assertNull(state.priorityFilter)
        // Ownership: the project is fetched scoped to the logged-in user.
        coVerify(exactly = 1) { projectRepository.getProject(userId, projectId) }
    }

    @Test
    fun `filtering by each status loads only matching tasks`() = runTest {
        coVerifyOwnershipReturns(project)
        every { taskRepository.filterByStatus(projectId, "TODO") } returns flowOf(listOf(todo))
        every { taskRepository.filterByStatus(projectId, "IN_PROGRESS") } returns
            flowOf(listOf(inProgress))
        every { taskRepository.filterByStatus(projectId, "DONE") } returns flowOf(listOf(done))
        val viewModel = build()

        viewModel.filterByStatus("TODO")
        assertEquals(listOf(todo), viewModel.uiState.value.tasks)
        assertEquals("TODO", viewModel.uiState.value.statusFilter)
        assertNull(viewModel.uiState.value.priorityFilter)

        viewModel.filterByStatus("IN_PROGRESS")
        assertEquals(listOf(inProgress), viewModel.uiState.value.tasks)
        assertEquals("IN_PROGRESS", viewModel.uiState.value.statusFilter)

        viewModel.filterByStatus("DONE")
        assertEquals(listOf(done), viewModel.uiState.value.tasks)
        assertEquals("DONE", viewModel.uiState.value.statusFilter)
    }

    @Test
    fun `clearing the status filter reloads all tasks`() = runTest {
        coVerifyOwnershipReturns(project)
        every { taskRepository.filterByStatus(projectId, "TODO") } returns flowOf(listOf(todo))
        val viewModel = build()
        viewModel.filterByStatus("TODO")
        assertEquals(listOf(todo), viewModel.uiState.value.tasks)

        viewModel.filterByStatus(null)

        assertNull(viewModel.uiState.value.statusFilter)
        assertEquals(allTasks, viewModel.uiState.value.tasks)
    }

    @Test
    fun `filtering by priority replaces an active status filter`() = runTest {
        coVerifyOwnershipReturns(project)
        every { taskRepository.filterByStatus(projectId, "TODO") } returns flowOf(listOf(todo))
        every { taskRepository.filterByPriority(projectId, "HIGH") } returns flowOf(listOf(done))
        val viewModel = build()
        viewModel.filterByStatus("TODO")

        viewModel.filterByPriority("HIGH")

        assertEquals("HIGH", viewModel.uiState.value.priorityFilter)
        assertNull(viewModel.uiState.value.statusFilter)
        assertEquals(listOf(done), viewModel.uiState.value.tasks)
    }

    @Test
    fun `toggleTaskStatus marks an open task done`() = runTest {
        coVerifyOwnershipReturns(project)
        val viewModel = build()

        viewModel.toggleTaskStatus(todo)

        coVerify(exactly = 1) {
            taskRepository.updateTask(todo.copy(isCompleted = true, status = "DONE"))
        }
    }

    @Test
    fun `toggleTaskStatus reopens a completed task`() = runTest {
        coVerifyOwnershipReturns(project)
        val viewModel = build()

        viewModel.toggleTaskStatus(done)

        coVerify(exactly = 1) {
            taskRepository.updateTask(done.copy(isCompleted = false, status = "TODO"))
        }
    }

    @Test
    fun `deleteTask delegates to the repository`() = runTest {
        coVerifyOwnershipReturns(project)
        val viewModel = build()

        viewModel.deleteTask(2L)

        coVerify(exactly = 1) { taskRepository.deleteTask(2L) }
    }

    /** Stubs the ownership-scoped project read used by `loadProject()`. */
    private fun coVerifyOwnershipReturns(result: ProjectEntity?) {
        io.mockk.coEvery { projectRepository.getProject(userId, projectId) } returns result
    }
}