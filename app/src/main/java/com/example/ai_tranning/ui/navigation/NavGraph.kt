package com.example.ai_tranning.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ai_tranning.ui.screens.dashboard.DashboardScreen
import com.example.ai_tranning.ui.screens.login.LoginScreen
import com.example.ai_tranning.ui.screens.project.ProjectScreen
import com.example.ai_tranning.ui.screens.register.RegisterScreen
import com.example.ai_tranning.ui.screens.task.TaskScreen

/**
 * Central registry of navigation routes.
 *
 * `const` entries are route *patterns* (with `{arg}` placeholders) used to register destinations;
 * the helper functions build concrete, argument-filled paths for navigation calls.
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
    const val PROJECT = "project/{projectId}"
    const val TASK_CREATE = "task/create/{projectId}"
    const val TASK_EDIT = "task/edit/{projectId}/{taskId}"

    /** Builds the route to a project's detail screen. */
    fun project(projectId: Long) = "project/$projectId"

    /** Builds the route to the task-creation screen for a project. */
    fun taskCreate(projectId: Long) = "task/create/$projectId"

    /** Builds the route to the edit screen for an existing task. */
    fun taskEdit(projectId: Long, taskId: Long) = "task/edit/$projectId/$taskId"
}

/**
 * Declares the app's navigation graph and wires each screen's navigation callbacks.
 *
 * Defines destinations for login, register, dashboard, project detail, and task create/edit,
 * including the typed `Long` arguments (`projectId`, `taskId`) the detail screens require.
 *
 * @param navController controller that performs the actual navigation.
 * @param startDestination the initial route (chosen by `MainActivity` from the session state).
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onProjectClick = { projectId ->
                    navController.navigate(Routes.project(projectId))
                },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.PROJECT,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) {
            ProjectScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddTask = { projectId ->
                    navController.navigate(Routes.taskCreate(projectId))
                },
                onEditTask = { projectId, taskId ->
                    navController.navigate(Routes.taskEdit(projectId, taskId))
                }
            )
        }

        composable(
            route = Routes.TASK_CREATE,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) {
            TaskScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.TASK_EDIT,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("taskId") { type = NavType.LongType }
            )
        ) {
            TaskScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}