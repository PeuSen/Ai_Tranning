package com.example.ai_tranning.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.ai_tranning.data.local.entity.TaskEntity
import com.example.ai_tranning.data.repository.TaskRepository
import com.example.ai_tranning.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [TaskViewModel] covering both create and edit modes.
 *
 * [TaskRepository] is mocked; the [MainDispatcherRule] makes the load (edit mode) and save coroutines
 * run synchronously. `projectId`/`taskId` are supplied through a real [SavedStateHandle] exactly as
 * the navigation arguments would be.
 *
 * Coverage:
 *  - Create mode: default state, blank-title validation, successful create with all fields.
 *  - Edit mode: existing task is loaded into the form; saving updates it and derives `isCompleted`
 *    from a `DONE` status.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val projectId = 1L
    private lateinit var taskRepository: TaskRepository

    private fun createModeViewModel(): TaskViewModel {
        taskRepository = mockk(relaxed = true)
        val handle = SavedStateHandle(mapOf("projectId" to projectId))
        return TaskViewModel(handle, taskRepository)
    }

    private fun editModeViewModel(existing: TaskEntity): TaskViewModel {
        taskRepository = mockk(relaxed = true)
        coEvery { taskRepository.getTask(existing.id) } returns existing
        val handle = SavedStateHandle(mapOf("projectId" to projectId, "taskId" to existing.id))
        return TaskViewModel(handle, taskRepository)
    }

    @Test
    fun `create mode starts with default state`() {
        val viewModel = createModeViewModel()

        val state = viewModel.uiState.value
        assertEquals("", state.title)
        assertEquals("TODO", state.status)
        assertEquals("MEDIUM", state.priority)
        assertFalse(state.isEditing)
        assertFalse(state.isSaved)
    }

    @Test
    fun `saveTask with blank title shows an error and does not persist`() = runTest {
        val viewModel = createModeViewModel()
        viewModel.onTitleChange("   ")

        viewModel.saveTask()

        assertEquals("Task title is required", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaved)
        coVerify(exactly = 0) { taskRepository.createTask(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `saveTask creates a new task with the entered fields`() = runTest {
        val viewModel = createModeViewModel()
        viewModel.onTitleChange("Write tests")
        viewModel.onDescriptionChange("cover the VM")
        viewModel.onPriorityChange("HIGH")
        viewModel.onDueDateChange(123L)

        viewModel.saveTask()

        coVerify(exactly = 1) {
            taskRepository.createTask(
                projectId = projectId,
                title = "Write tests",
                description = "cover the VM",
                priority = "HIGH",
                dueDate = 123L
            )
        }
        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.isLoading)
    }

    @Test
    fun `edit mode loads the existing task into the form`() = runTest {
        val existing = TaskEntity(
            id = 5,
            projectId = projectId,
            title = "Existing",
            description = "old",
            status = "IN_PROGRESS",
            priority = "LOW",
            dueDate = 999L
        )

        val viewModel = editModeViewModel(existing)

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertEquals("Existing", state.title)
        assertEquals("old", state.description)
        assertEquals("IN_PROGRESS", state.status)
        assertEquals("LOW", state.priority)
        assertEquals(999L, state.dueDate)
    }

    @Test
    fun `saveTask updates the existing task and derives isCompleted from DONE`() = runTest {
        val existing = TaskEntity(
            id = 5,
            projectId = projectId,
            title = "Existing",
            description = "old",
            status = "IN_PROGRESS",
            priority = "LOW"
        )
        val viewModel = editModeViewModel(existing)
        viewModel.onStatusChange("DONE")

        viewModel.saveTask()

        coVerify(exactly = 1) {
            taskRepository.updateTask(existing.copy(status = "DONE", isCompleted = true))
        }
        assertTrue(viewModel.uiState.value.isSaved)
    }
}