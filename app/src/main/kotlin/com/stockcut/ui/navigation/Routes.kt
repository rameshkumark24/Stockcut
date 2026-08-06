package com.stockcut.ui.navigation

/**
 * The five routes from docs/03-app-flow.md.
 *
 * ```
 * projects                 Projects list (start destination)
 * project/{id}             Project editor  — tabs: Parts · Stock · Setup
 * project/{id}/result      Cut plan
 * settings                 Global settings
 * about                    About / support / legal
 * ```
 *
 * Sheets are NOT routes — AddPartSheet, AddStockSheet, UnitPickerSheet,
 * PaywallSheet and DeleteConfirmDialog are all composables owned by the screen
 * that raises them. Making them destinations would put the paywall in the back
 * stack, where pressing Back after a purchase would show it again.
 */
object Routes {
    const val PROJECTS = "projects"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    const val PROJECT_ARG = "projectId"
    const val PROJECT = "project/{$PROJECT_ARG}"
    const val RESULT = "project/{$PROJECT_ARG}/result"

    fun project(id: Long): String = "project/$id"
    fun result(id: Long): String = "project/$id/result"
}
