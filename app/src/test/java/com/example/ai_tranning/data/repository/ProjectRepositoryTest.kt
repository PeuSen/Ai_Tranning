package com.example.ai_tranning.data.repository

import com.example.ai_tranning.data.local.dao.ProjectDao
import com.example.ai_tranning.data.local.entity.ProjectEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ProjectRepository] with a mocked [ProjectDao].
 *
 * These isolate the repository's own logic — ownership-scoped reads/writes and the partial-update
 * (PATCH) merge semantics — from Room. Cross-user isolation is end-to-end-tested separately in
 * `ProjectRepositoryOwnershipTest` (instrumented).
 *
 * Coverage:
 *  - `getProjects` / `getProject` delegate to the ownership-scoped DAO queries.
 *  - `getProject` returns null for a project the user does not own.
 *  - `updateProject` applies only the supplied fields and skips the write when not owned.
 *  - `deleteProject` reports success based on the DAO's affected-row count.
 *  - `createProject` persists the color and returns the new id.
 */
class ProjectRepositoryTest {

    private val userId = 7L
    private lateinit var dao: ProjectDao
    private lateinit var repository: ProjectRepository

    private val owned = ProjectEntity(
        id = 1, userId = userId, name = "Owned", description = "desc", color = "#112233"
    )

    @Before
    fun setUp() {
        dao = mockk()
        repository = ProjectRepository(dao)
    }

    @Test
    fun `getProjects delegates to the per-user query`() = runTest {
        every { dao.getProjectsByUser(userId) } returns flowOf(listOf(owned))

        assertEquals(listOf(owned), repository.getProjects(userId).first())
    }

    @Test
    fun `getProject returns the project when the user owns it`() = runTest {
        coEvery { dao.getProjectByIdForUser(1, userId) } returns owned

        assertEquals(owned, repository.getProject(userId, 1))
    }

    @Test
    fun `getProject returns null when the user does not own it`() = runTest {
        coEvery { dao.getProjectByIdForUser(1, userId) } returns null

        assertNull(repository.getProject(userId, 1))
    }

    @Test
    fun `updateProject applies only the supplied fields`() = runTest {
        coEvery { dao.getProjectByIdForUser(1, userId) } returns owned
        val saved = slot<ProjectEntity>()
        coEvery { dao.updateProject(capture(saved)) } returns Unit

        val result = repository.updateProject(userId, 1, name = "Renamed")

        assertTrue(result)
        assertEquals("Renamed", saved.captured.name)
        assertEquals("desc", saved.captured.description)   // unchanged
        assertEquals("#112233", saved.captured.color)      // unchanged
    }

    @Test
    fun `updateProject can change only the color`() = runTest {
        coEvery { dao.getProjectByIdForUser(1, userId) } returns owned
        val saved = slot<ProjectEntity>()
        coEvery { dao.updateProject(capture(saved)) } returns Unit

        repository.updateProject(userId, 1, color = "#FF0000")

        assertEquals("Owned", saved.captured.name)         // unchanged
        assertEquals("#FF0000", saved.captured.color)
    }

    @Test
    fun `updateProject returns false and skips the write when not owned`() = runTest {
        coEvery { dao.getProjectByIdForUser(1, userId) } returns null

        val result = repository.updateProject(userId, 1, name = "hacked")

        assertFalse(result)
        coVerify(exactly = 0) { dao.updateProject(any()) }
    }

    @Test
    fun `deleteProject returns true when a row was deleted`() = runTest {
        coEvery { dao.deleteProjectByIdForUser(1, userId) } returns 1

        assertTrue(repository.deleteProject(userId, 1))
    }

    @Test
    fun `deleteProject returns false when nothing was deleted`() = runTest {
        coEvery { dao.deleteProjectByIdForUser(1, userId) } returns 0

        assertFalse(repository.deleteProject(userId, 1))
    }

    @Test
    fun `createProject persists the fields and returns the new id`() = runTest {
        val saved = slot<ProjectEntity>()
        coEvery { dao.insertProject(capture(saved)) } returns 10L

        val id = repository.createProject(userId, "New", "Desc", "#00FF00")

        assertEquals(10L, id)
        assertEquals(userId, saved.captured.userId)
        assertEquals("New", saved.captured.name)
        assertEquals("Desc", saved.captured.description)
        assertEquals("#00FF00", saved.captured.color)
    }
}