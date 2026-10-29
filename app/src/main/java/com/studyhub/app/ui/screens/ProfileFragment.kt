package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyhub.app.R
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.databinding.FragmentProfileBinding
import com.studyhub.app.ui.components.DialogHelper
import com.studyhub.app.ui.navigation.Routes
import com.studyhub.app.viewmodel.MainUiState

class ProfileFragment : HubFragment(R.layout.fragment_profile) {
    private var profile: UserProfile? = null
    private var currentLanguage: String = "English"
    private var applyingSettings = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentProfileBinding.bind(view)
        binding.buttonEditProfile.setOnClickListener {
            profile?.let { user ->
                DialogHelper.editProfile(requireContext(), user) { name, course, email ->
                    mainViewModel.updateProfile(name, course, email)
                }
            }
        }
        binding.buttonLanguage.setOnClickListener {
            val languages = arrayOf("English", "Afrikaans", "isiZulu")
            val selected = languages.indexOf(currentLanguage).coerceAtLeast(0)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Choose language")
                .setSingleChoiceItems(languages, selected) { dialog, which ->
                    currentLanguage = languages[which]
                    settingsViewModel.setLanguage(currentLanguage)
                    binding.buttonLanguage.text = "Language · $currentLanguage"
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.buttonSignOut.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign out?")
                .setMessage("You can sign back in at any time.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sign out") { _, _ ->
                    authViewModel.signOut()
                    navigator.navigate(Routes.WELCOME, clearBackStack = true)
                }
                .show()
        }
        binding.switchDarkMode.setOnCheckedChangeListener { _, checked -> if (!applyingSettings) settingsViewModel.setDarkMode(checked) }
        binding.switchOfflineSync.setOnCheckedChangeListener { _, checked -> if (!applyingSettings) settingsViewModel.setOfflineSync(checked) }
        binding.switchDataSaver.setOnCheckedChangeListener { _, checked -> if (!applyingSettings) settingsViewModel.setDataSaver(checked) }
        binding.switchNotifications.setOnCheckedChangeListener { _, checked -> if (!applyingSettings) settingsViewModel.setNotifications(checked) }

        observe(mainViewModel.state) { state ->
            profile = state.profile
            renderProfile(binding, state)
        }
        observe(settingsViewModel.state) { state ->
            currentLanguage = state.language
            binding.buttonLanguage.text = "Language · ${state.language}"
            applyingSettings = true
            binding.switchDarkMode.isChecked = state.darkMode
            binding.switchOfflineSync.isChecked = state.offlineSync
            binding.switchDataSaver.isChecked = state.dataSaver
            binding.switchNotifications.isChecked = state.notifications
            applyingSettings = false
        }
    }

    private fun renderProfile(binding: FragmentProfileBinding, state: MainUiState) {
        val user = state.profile
        binding.textProfileName.text = user?.name?.ifBlank { "Student" } ?: "Student"
        binding.textProfileEmail.text = user?.email.orEmpty()
        binding.textProfileCourse.text = "${user?.course?.ifBlank { "Information Technology" } ?: "Information Technology"} · ${user?.yearOfStudy?.ifBlank { "3rd Year" } ?: "3rd Year"}"
        binding.textProfileStats.text = "${user?.points ?: 0} points · ${user?.tasksCompleted ?: state.completedTasks.size} tasks completed · ${state.groups.size} groups"
    }
}
