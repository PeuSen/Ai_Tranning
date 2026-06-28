
package com.example.ai_tranning.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ai_tranning.ui.theme.DangerRed
import com.example.ai_tranning.viewmodel.DashboardViewModel
import com.example.ai_tranning.viewmodel.ProjectWithCount

/**
 * Dashboard screen listing the logged-in user's projects with task-progress indicators.
 *
 * Shows a loading spinner, an empty-state message, or the project list, plus a FAB and dialog for
 * creating/editing projects. State is provided by [DashboardViewModel].
 *
 * @param onProjectClick called with a project id when a project card is tapped.
 * @param onLoggedOut called after logout completes, to navigate away from the dashboard.
 * @param viewModel the backing [DashboardViewModel]; defaults to a Hilt-provided instance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onProjectClick: (Long) -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreateDialog) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.projects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No projects yet. Tap + to create one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.projects, key = { it.project.id }) { item ->
                    ProjectCard(
                        item = item,
                        onClick = { onProjectClick(item.project.id) },
                        onEdit = { viewModel.showEditDialog(item.project) },
                        onDelete = { viewModel.deleteProject(item.project.id) }
                    )
                }
            }
        }
    }

    if (uiState.showCreateDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = {
                Text(if (uiState.editingProject != null) "Edit Project" else "New Project")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.projectName,
                        onValueChange = viewModel::onProjectNameChange,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.projectDescription,
                        onValueChange = viewModel::onProjectDescriptionChange,
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    uiState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveProject) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDialog) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Card showing a single project's name, description, completion count, and progress bar, with edit
 * and delete actions.
 *
 * @param item the project paired with its task-progress counts.
 * @param onClick called when the card body is tapped.
 * @param onEdit called when the edit action is tapped.
 * @param onDelete called when the delete action is tapped.
 */
@Composable
private fun ProjectCard(
    item: ProjectWithCount,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProjectColorDot(colorHex = item.project.color)
                Text(
                    text = item.project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                }
            }

            if (item.project.description.isNotBlank()) {
                Text(
                    text = item.project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "${item.completedCount}/${item.taskCount} tasks completed",
                style = MaterialTheme.typography.bodySmall
            )
            if (item.taskCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { item.completedCount.toFloat() / item.taskCount },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Small circular swatch showing a project's [colorHex] (e.g. `"#FF6200EE"`). Renders nothing when
 * the color is `null` or cannot be parsed, so an uncolored project simply has no dot.
 *
 * @param colorHex the project's stored color string, or `null`.
 */
@Composable
private fun ProjectColorDot(colorHex: String?) {
    val parsed = colorHex?.let { hex ->
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
    } ?: return

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(parsed)
    )
}