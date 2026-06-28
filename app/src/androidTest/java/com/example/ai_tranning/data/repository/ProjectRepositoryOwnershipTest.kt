package com.example.ai_tranning.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ai_tranning.data.local.AppDatabase
import com.example.ai_tranning.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end ownership tests for [ProjectRepository] wired to a real (in-memory) Room database.
 *
 * These verify the cross-user isolation requirement: a user must never be able to view, edit, or
 * delete another user's project. Each of those operations is the offline-app equivalent of a guarded
 * REST endpoint (`GET` / `PATCH` / `DELETE /projects/{id}`), and a non-owned project is treated
 * exactly like a missing one — `getProjects` omits it, `getProject` returns `null`, and the
 * update/delete calls report `false` without touching the row.
 *
 * The primary test reproduces the required scenario: register user A and user B, have A create one
 * project, then assert B cannot list, read, patch, or delete it. Companion tests cover the owner's
 * happy path and cascade deletion of tasks.
 */
@RunWith(AndroidJUnit4::class)
class ProjectRepositoryOwnershipTest {

    private lateinit var db: AppDatabase
    private lateinit var users: UserRepository
    private lateinit var projects: ProjectRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        users = UserRepository(db.userDao())
        projects = ProjectRepository(db.projectDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun userB_cannotListReadPatchOrDeleteUserAsProject() = runTest {
        // 1 & 2. Register user A and user B.
        val userA = users.registerUser("alice", "alice@example.com", "password123").getOrThrow()
        val userB = users.registerUser("bob", "bob@example.com", "password123").getOrThrow()

        // 3. Both users log in successfully.
        assertTrue(users.loginUser("alice", "password123").isSuccess)
        assertTrue(users.loginUser("bob", "password123").isSuccess)

        // 4. User A creates one project.
        val projectId = projects.createProject(userA, "Alice's project", "secret")

        // 5. User B lists projects -> expect an empty list.
        val bobsProjects = projects.getProjects(userB).first()
        assertEquals(0, bobsProjects.size)

        // 6. User B requests GET /projects/{id} -> not found (null).
        assertNull(projects.getProject(userB, projectId))

        // 7. User B sends PATCH /projects/{id} -> rejected, and nothing is changed.
        val patched = projects.updateProject(userB, projectId, name = "hacked", color = "#000000")
        assertFalse(patched)
        val afterPatch = projects.getProject(userA, projectId)
        assertEquals("Alice's project", afterPatch?.name)
        assertNull(afterPatch?.color)

        // 8. User B sends DELETE /projects/{id} -> rejected, and the project still exists.
        val deleted = projects.deleteProject(userB, projectId)
        assertFalse(deleted)
        assertNotNull(projects.getProject(userA, projectId))
    }

    @Test
    fun owner_canReadPatchAndDeleteOwnProject() = runTest {
        val userA = users.registerUser("alice", "alice@example.com", "password123").getOrThrow()
        val projectId = projects.createProject(userA, "Alice's project", "desc")

        // Owner lists and reads their own project.
        assertEquals(1, projects.getProjects(userA).first().size)
        assertEquals("Alice's project", projects.getProject(userA, projectId)?.name)

        // PATCH accepts any subset of name/description/color; omitted fields are left unchanged.
        assertTrue(projects.updateProject(userA, projectId, name = "Renamed", color = "#FF0000"))
        val updated = projects.getProject(userA, projectId)
        assertEquals("Renamed", updated?.name)
        assertEquals("desc", updated?.description) // untouched
        assertEquals("#FF0000", updated?.color)

        // DELETE removes the project for its owner.
        assertTrue(projects.deleteProject(userA, projectId))
        assertNull(projects.getProject(userA, projectId))
    }

    @Test
    fun deletingProject_alsoRemovesItsTasks() = runTest {
        val userA = users.registerUser("alice", "alice@example.com", "password123").getOrThrow()
        val projectId = projects.createProject(userA, "Alice's project", "desc")
        db.taskDao().insertTask(TaskEntity(projectId = projectId, title = "task 1"))
        db.taskDao().insertTask(TaskEntity(projectId = projectId, title = "task 2"))
        assertEquals(2, db.taskDao().getTaskCount(projectId).first())

        assertTrue(projects.deleteProject(userA, projectId))

        // Cascade delete removed the project's tasks.
        assertEquals(0, db.taskDao().getTaskCount(projectId).first())
    }
}