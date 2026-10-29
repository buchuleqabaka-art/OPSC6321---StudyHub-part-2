package com.studyhub.app.ui.screens

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.studyhub.app.ui.navigation.StudyHubNavigator
import com.studyhub.app.viewmodel.AuthViewModel
import com.studyhub.app.viewmodel.MainViewModel
import com.studyhub.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Shared activity-scoped state and lifecycle-safe Flow collection for XML screens. */
abstract class HubFragment(@LayoutRes layoutId: Int) : Fragment(layoutId) {
    protected val mainViewModel: MainViewModel by activityViewModels()
    protected val authViewModel: AuthViewModel by activityViewModels()
    protected val settingsViewModel: SettingsViewModel by activityViewModels()

    protected val navigator: StudyHubNavigator
        get() = requireActivity() as StudyHubNavigator

    protected fun <T> observe(flow: Flow<T>, onValue: (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collect { value -> onValue(value) }
            }
        }
    }
}
