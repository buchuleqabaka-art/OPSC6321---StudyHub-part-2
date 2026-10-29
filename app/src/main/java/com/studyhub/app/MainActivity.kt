package com.studyhub.app

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.studyhub.app.databinding.ActivityMainBinding
import com.studyhub.app.ui.navigation.Routes
import com.studyhub.app.ui.navigation.StudyHubNavigator
import com.studyhub.app.ui.screens.CalendarFragment
import com.studyhub.app.ui.screens.ChatFragment
import com.studyhub.app.ui.screens.GroupsFragment
import com.studyhub.app.ui.screens.HomeFragment
import com.studyhub.app.ui.screens.LoginFragment
import com.studyhub.app.ui.screens.ProfileFragment
import com.studyhub.app.ui.screens.RegisterFragment
import com.studyhub.app.ui.screens.SessionsFragment
import com.studyhub.app.ui.screens.TasksFragment
import com.studyhub.app.ui.screens.WelcomeFragment
import com.studyhub.app.viewmodel.AuthViewModel
import com.studyhub.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), StudyHubNavigator {
    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var currentRoute: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val route = when (item.itemId) {
                R.id.nav_groups -> Routes.GROUPS
                R.id.nav_tasks -> Routes.TASKS
                R.id.nav_sessions -> Routes.SESSIONS
                R.id.nav_profile -> Routes.PROFILE
                else -> Routes.HOME
            }
            navigate(route, clearBackStack = true)
            true
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val visible = supportFragmentManager.findFragmentById(R.id.fragment_container)
            visible?.tag?.let { currentRoute = it }
            updateNavigationChrome()
        }

        if (savedInstanceState == null) {
            val firstRoute = if (authViewModel.state.value.signedIn) Routes.HOME else Routes.WELCOME
            navigate(firstRoute, clearBackStack = true)
        } else {
            currentRoute = supportFragmentManager.findFragmentById(R.id.fragment_container)?.tag
                ?: Routes.WELCOME
            updateNavigationChrome()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.state.collect { state ->
                        if (state.signedIn && currentRoute in AUTH_ROUTES) {
                            navigate(Routes.HOME, clearBackStack = true)
                        }
                    }
                }
                launch {
                    settingsViewModel.state.collect { state ->
                        val requestedMode = if (state.darkMode) {
                            AppCompatDelegate.MODE_NIGHT_YES
                        } else {
                            AppCompatDelegate.MODE_NIGHT_NO
                        }
                        if (AppCompatDelegate.getDefaultNightMode() != requestedMode) {
                            AppCompatDelegate.setDefaultNightMode(requestedMode)
                        }
                    }
                }
            }
        }
    }

    override fun navigate(route: String, clearBackStack: Boolean) {
        if (!::binding.isInitialized) return
        if (clearBackStack) {
            supportFragmentManager.popBackStackImmediate(
                null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
        if (!clearBackStack && currentRoute == route) return

        val fragment = when (route) {
            Routes.LOGIN -> LoginFragment()
            Routes.REGISTER -> RegisterFragment()
            Routes.HOME -> HomeFragment()
            Routes.GROUPS -> GroupsFragment()
            Routes.TASKS -> TasksFragment()
            Routes.CALENDAR -> CalendarFragment()
            Routes.SESSIONS -> SessionsFragment()
            Routes.CHATS -> ChatFragment()
            Routes.PROFILE -> ProfileFragment()
            else -> WelcomeFragment()
        }

        val transaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, fragment, route)
        if (!clearBackStack && currentRoute.isNotEmpty()) {
            transaction.addToBackStack(route)
        }
        currentRoute = route
        transaction.commit()
        updateNavigationChrome()
    }

    private fun updateNavigationChrome() {
        if (!::binding.isInitialized) return
        val mainScreen = currentRoute in MAIN_ROUTES
        binding.bottomNavigation.visibility = if (mainScreen) View.VISIBLE else View.GONE
        val selectedItem = when (currentRoute) {
            Routes.GROUPS -> R.id.nav_groups
            Routes.TASKS -> R.id.nav_tasks
            Routes.SESSIONS -> R.id.nav_sessions
            Routes.PROFILE -> R.id.nav_profile
            else -> R.id.nav_home
        }
        binding.bottomNavigation.menu.findItem(selectedItem)?.isChecked = mainScreen
    }

    companion object {
        private val AUTH_ROUTES = setOf(Routes.WELCOME, Routes.LOGIN, Routes.REGISTER)
        private val MAIN_ROUTES = setOf(
            Routes.HOME,
            Routes.GROUPS,
            Routes.TASKS,
            Routes.SESSIONS,
            Routes.PROFILE
        )
    }
}
