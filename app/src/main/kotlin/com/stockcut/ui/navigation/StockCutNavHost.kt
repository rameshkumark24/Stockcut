package com.stockcut.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stockcut.AppContainer
import com.stockcut.ui.editor.ProjectEditorScreen
import com.stockcut.ui.editor.ProjectEditorViewModel
import com.stockcut.ui.about.AboutScreen
import com.stockcut.ui.settings.SettingsScreen
import com.stockcut.ui.settings.SettingsViewModel
import com.stockcut.ui.result.ResultScreen
import com.stockcut.ui.result.ResultViewModel
import com.stockcut.ui.projects.ProjectsScreen
import com.stockcut.ui.projects.ProjectsViewModel
import com.stockcut.ui.theme.Space

/**
 * The navigation graph — five routes, per docs/03-app-flow.md.
 *
 * `projects` is the start destination. Three screens are still stubs; they are
 * wired now so the graph is complete and each one can be filled in without
 * touching navigation again.
 */
@Composable
fun StockCutNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.PROJECTS,
        modifier = modifier,
    ) {
        composable(Routes.PROJECTS) {
            val viewModel: ProjectsViewModel = viewModel(
                factory = ProjectsViewModel.factory(container),
            )
            ProjectsScreen(
                viewModel = viewModel,
                onOpenProject = { id -> navController.navigate(Routes.project(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
            )
        }

        composable(
            route = Routes.PROJECT,
            arguments = listOf(navArgument(Routes.PROJECT_ARG) { type = NavType.LongType }),
        ) { entry ->
            val projectId = entry.arguments?.getLong(Routes.PROJECT_ARG) ?: 0L
            val viewModel: ProjectEditorViewModel = viewModel(
                // Keyed by project id, so opening a second job does not reuse the
                // first one's ViewModel and show its parts.
                key = "editor-$projectId",
                factory = ProjectEditorViewModel.factory(container, projectId),
            )
            ProjectEditorScreen(
                viewModel = viewModel,
                onOptimize = { navController.navigate(Routes.result(projectId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(navArgument(Routes.PROJECT_ARG) { type = NavType.LongType }),
        ) { entry ->
            val projectId = entry.arguments?.getLong(Routes.PROJECT_ARG) ?: 0L
            val viewModel: ResultViewModel = viewModel(
                key = "result-$projectId",
                factory = ResultViewModel.factory(container, projectId),
            )
            ResultScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(container),
            )
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(container),
            )
            AboutScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

/** Placeholder for a route that exists but is not built yet. */
@Composable
private fun Stub(title: String, detail: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
