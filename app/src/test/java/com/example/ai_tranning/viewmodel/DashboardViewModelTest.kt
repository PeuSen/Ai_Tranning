package com.example.ai_tranning.viewmodel

import com.example.ai_tranning.data.local.entity.ProjectEntity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [DashboardViewModel].
 *
 * All collaborators are mocked. The [MainDispatcherRule]'s unconfined dispatcher makes the project
 * load launched in `init` run synchronously, so the resulting [DashboardUiState] can be asserted
 * right after the view model is constructed.
 *
 * Coverage:
 *  - Only the logged-in user's projects are loaded (ownership), enriched with task counts.
 *  - Empty project list resolves loading to an empty state.
 *  - Create/edit dialog state transitions.
 *  - Validation: a blank project name is rejected before any repository write.
 *  - Create and edit both delegate to the repository with the logged-in user id.
 *  - Delete and logout delegate correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = 7L
    private lateinit var projectRepository: ProjectRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        projectRepository = mockk(relaxed = true)
        taskRepository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        // Sensible defaults; individual tests override getProjects where needed.
        every { sessionManager.loggedInUserId } returns flowOf(userId)
        every { projectRepository.getProjects(userId) } returns flowOf(emptyList())
        every { taskRepository.getTaskCount(any()) } returns flowOf(0)
        every { taskRepository.getCompletedTaskCount(any()) } returns flowOf(0)
    }

    private fun build() = DashboardViewModel(projectRepository, taskRepository, sessionManager)

    @Test
    fun `loads only the logged-in users projects with task counts`() = runTest {
        val project = ProjectEntity(id = 1, userId = userId, name = "P1", description = "d")
        every { projectRepository.getProjects(userId) } returns flowOf(listOf(project))
        every { taskRepository.getTaskCount(1) } returns flowOf(3)
        every { taskRepository.getCompletedTaskCount(1) } returns flowOf(1)

        val viewModel = build()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.projects.size)
        assertEquals(project, state.projects[0].project)
        assertEquals(3, state.projects[0].taskCount)
        assertEquals(1, state.projects[0].completedCount)
        // Ownership: the dashboard only ever queries the logged-in user's projects.
        coVerify(exactly = 1) { projectRepository.getProjects(userId) }
    }

    @Test
    fun `empty project list resolves to an empty, idle state`() {
        val viewModel = build()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.projects.isEmpty())
    }

    @Test
    fun `showCreateDialog opens the dialog in create mode`() {
        val viewModel = build()

        viewModel.showCreateDialog()

        val state = viewModel.uiState.value
        assertTrue(state.showCreateDialog)
        assertNull(state.editingProject)
        assertEquals("", state.projectName)
    }

    @Test
    fun `showEditDialog prefills the dialog with the project values`() {
        val viewModel = build()
        val project = ProjectEntity(id = 5, userId = userId, name = "Old", description = "Desc")

        viewModel.showEditDialog(project)

        val state = viewModel.uiState.value
        assertTrue(state.showCreateDialog)
        assertEquals(project, state.editingProject)
        assertEquals("Old", state.projectName)
        assertEquals("Desc", state.projectDescription)
    }

    @Test
    fun `saveProject with blank name shows an error and does not persist`() = runTest {
        val viewModel = build()
        viewModel.showCreateDialog()
        viewModel.onProjectNameChange("   ")

        viewModel.saveProject()

        assertEquals("Project name is required", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { projectRepository.createProject(any(), any(), any(), any()) }
    }

    @Test
    fun `saveProject creates a new project for the logged-in user`() = runTest {
        val viewModel = build()
        viewModel.showCreateDialog()
        viewModel.onProjectNameChange("New")
        viewModel.onProjectDescriptionChange("Desc")

        viewModel.saveProject()

        coVerify(exactly = 1) { projectRepository.createProject(userId, "New", "Desc") }
        assertFalse(viewModel.uiState.value.showCreateDialog) // dismissed on success
    }

    @Test
    fun `saveProject updates the project being edited`() = runTest {
        val viewModel = build()
        val project = ProjectEntity(id = 5, userId = userId, name = "Old", description = "Desc")
        viewModel.showEditDialog(project)
        viewModel.onProjectNameChange("Renamed")

        viewModel.saveProject()

        coVerify(exactly = 1) {
            projectRepository.updateProject(userId, 5L, "Renamed", "Desc", null)
        }
        coVerify(exactly = 0) { projectRepository.createProject(any(), any(), any(), any()) }
    }

    @Test
    fun `deleteProject delegates with the logged-in user id (ownership)`() = runTest {
        val viewModel = build()

        viewModel.deleteProject(9L)

        coVerify(exactly = 1) { projectRepository.deleteProject(userId, 9L) }
    }

    @Test
    fun `logout clears the session and invokes the callback`() = runTest {
        val viewModel = build()
        var loggedOut = false

        viewModel.logout { loggedOut = true }

        assertTrue(loggedOut)
        coVerify(exactly = 1) { sessionManager.clearSession() }
    }
}