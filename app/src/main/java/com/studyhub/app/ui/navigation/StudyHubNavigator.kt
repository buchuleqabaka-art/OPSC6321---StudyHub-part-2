package com.studyhub.app.ui.navigation

interface StudyHubNavigator {
    fun navigate(route: String, clearBackStack: Boolean = false)
}
