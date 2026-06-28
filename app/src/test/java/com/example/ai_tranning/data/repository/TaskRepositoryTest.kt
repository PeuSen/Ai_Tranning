package com.example.ai_tranning.data.repository

import com.example.ai_tranning.data.local.dao.TaskDao
import com.example.ai_tranning.data.local.entity.TaskEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TaskRepository] with a mocked [TaskDao].
 *
 * The repository is a thin delegating layer, so these tests pin down the contract: each method calls
 * the right DAO query and forwards arguments/results unchanged — including the status/priority
 * filters that back the project screen's filter chips.
 *
 * Coverage:
 *  - `getTasks` / `getTask` reads.
 *  - `filterByStatus` and `filterByPriority` delegate to the matching DAO queries.
 *  - Task / completed-task counts delegate.
 *  - `createTask` persists the entity and returns the new id; `updateTask` / `deleteTask` delegate.
 */
class TaskRepositoryTest {

    private val projectId = 1L
    private lateinit var dao: TaskDao
    private lateinit var repository: TaskRepository

    private fun task(id: Long, status: String = "TODO", priority: String = "MEDIUM") =
        TaskEntity(id = id, projectId = projectId, title = "t$id", status = status, priority = priority)

    @Before
    fun setUp() {
        dao = mockk()
        repository = TaskRepository(dao)
    }

    @Test
    fun `getTasks delegates to the per-project query`() = runTest {
        val tasks = listOf(task(1), task(2))
        every { dao.getTasksByProject(projectId) } returns flowOf(tasks)

        assertEquals(tasks, repository.getTasks(projectId).first())
    }

    @Test
    fun `getTask returns the task for a known id`() = runTest {
        val t = task(5)
        coEvery { dao.getTaskById(5) } returns t

        assertEquals(t, repository.getTask(5))
    }

    @Test
    fun `getTask returns null for an unknown id`() = runTest {
        coEvery { dao.getTaskById(99) } returns null

        assertNull(repository.getTask(99))
    }

    @Test
    fun `filterByStatus delegates to the status query`() = runTest {
        val todos = listOf(task(1, status = "TODO"))
        every { dao.filterByStatus(projectId, "TODO") } returns flowOf(todos)

        assertEquals(todos, repository.filterByStatus(projectId, "TODO").first())
    }

    @Test
    fun `filterByPriority delegates to the priority query`() = runTest {
        val high = listOf(task(1, priority = "HIGH"))
        every { dao.filterByPriority(projectId, "HIGH") } returns flowOf(high)

        assertEquals(high, repository.filterByPriority(projectId, "HIGH").first())
    }

    @Test
    fun `task counts delegate to the DAO`() = runTest {
        every { dao.getTaskCount(projectId) } returns flowOf(5)
        every { dao.getCompletedTaskCount(projectId) } returns flowOf(2)

        assertEquals(5, repository.getTaskCount(projectId).first())
        assertEquals(2, repository.getCompletedTaskCount(projectId).first())
    }

    @Test
    fun `createTask persists the entity and returns the new id`() = runTest {
        val saved = slot<TaskEntity>()
        coEvery { dao.insertTask(capture(saved)) } returns 11L

        val id = repository.createTask(projectId, "New", "desc", "HIGH", 42L)

        assertEquals(11L, id)
        assertEquals(projectId, saved.captured.projectId)
        assertEquals("New", saved.captured.title)
        assertEquals("desc", saved.captured.description)
        assertEquals("HIGH", saved.captured.priority)
        assertEquals(42L, saved.captured.dueDate)
    }

    @Test
    fun `updateTask delegates to the DAO`() = runTest {
        val t = task(3)
        coEvery { dao.updateTask(t) } returns Unit

        repository.updateTask(t)

        coVerify(exactly = 1) { dao.updateTask(t) }
    }

    @Test
    fun `deleteTask delegates to the DAO`() = runTest {
        coEvery { dao.deleteTaskById(4) } returns Unit

        repository.deleteTask(4)

        coVerify(exactly = 1) { dao.deleteTaskById(4) }
    }
}