package com.studyhub.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.ImageView
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyhub.app.R
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.databinding.FragmentProfileBinding
import com.studyhub.app.notifications.PushRegistration
import com.studyhub.app.ui.components.DialogHelper
import com.studyhub.app.ui.navigation.Routes
import com.studyhub.app.util.AppLanguages
import com.studyhub.app.util.PlanningPolicy
import com.studyhub.app.viewmodel.MainUiState
import kotlinx.coroutines.launch

class ProfileFragment : HubFragment(R.layout.fragment_profile) {
    private var profile: UserProfile? = null
    private var currentLanguage = AppLanguages.ENGLISH
    private var applyingSettings = false
    private val profilePhotoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            view?.findViewById<ImageView>(R.id.image_profile)?.apply {
                setImageURI(uri)
                tag = uri.toString()
            }
            mainViewModel.uploadProfilePhoto(uri) { result ->
                view?.let { root ->
                    val message = if (result.isSuccess) {
                        R.string.profile_photo_upload_success
                    } else {
                        R.string.profile_photo_upload_failed
                    }
                    com.google.android.material.snackbar.Snackbar.make(
                        root,
                        message,
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            settingsViewModel.setNotifications(false)
            lifecycleScope.launch { PushRegistration.unregister() }
            view?.let { root ->
                com.google.android.material.snackbar.Snackbar.make(
                    root,
                    R.string.notification_permission_denied,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentProfileBinding.bind(view)
        binding.buttonUploadProfilePhoto.setOnClickListener {
            profilePhotoPicker.launch("image/*")
        }
        binding.buttonEditProfile.setOnClickListener {
            profile?.let { user ->
                DialogHelper.editProfile(requireContext(), user) { name, course, email ->
                    mainViewModel.updateProfile(name, course, email)
                }
            }
        }
        binding.buttonLanguage.setOnClickListener {
            val options = AppLanguages.supported
            val labels = options.map { getString(it.labelRes) }.toTypedArray()
            val selected = options.indexOfFirst { it.tag == currentLanguage }.coerceAtLeast(0)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.choose_language)
                .setSingleChoiceItems(labels, selected) { dialog, which ->
                    currentLanguage = options[which].tag
                    settingsViewModel.setLanguage(currentLanguage)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        binding.buttonSignOut.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sign_out_confirm)
                .setMessage(R.string.sign_out_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.sign_out) { _, _ ->
                    lifecycleScope.launch {
                        PushRegistration.unregister()
                        authViewModel.signOut()
                        navigator.navigate(Routes.WELCOME, clearBackStack = true)
                    }
                }
                .show()
        }
        binding.switchDarkMode.setOnCheckedChangeListener { _, checked ->
            if (!applyingSettings) settingsViewModel.setDarkMode(checked)
        }
        binding.switchOfflineSync.setOnCheckedChangeListener { _, checked ->
            if (!applyingSettings) settingsViewModel.setOfflineSync(checked)
        }
        binding.switchDataSaver.setOnCheckedChangeListener { _, checked ->
            if (!applyingSettings) settingsViewModel.setDataSaver(checked)
        }
        binding.switchNotifications.setOnCheckedChangeListener { _, checked ->
            if (!applyingSettings) {
                settingsViewModel.setNotifications(checked)
                if (checked) {
                    requestNotificationPermissionIfNeeded()
                } else {
                    lifecycleScope.launch { PushRegistration.unregister() }
                }
            }
        }

        observe(mainViewModel.state) { state ->
            profile = state.profile
            renderProfile(binding, state)
        }
        observe(settingsViewModel.state) { state ->
            currentLanguage = AppLanguages.normalize(state.language)
            binding.buttonLanguage.text = getString(
                R.string.language_format,
                getString(AppLanguages.labelFor(currentLanguage))
            )
            applyingSettings = true
            binding.switchDarkMode.isChecked = state.darkMode
            binding.switchOfflineSync.isChecked = state.offlineSync
            binding.switchDataSaver.isChecked = state.dataSaver
            binding.switchNotifications.isChecked = state.notifications
            applyingSettings = false
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun renderProfile(binding: FragmentProfileBinding, state: MainUiState) {
        val user = state.profile
        val photoUrl = user?.photoUrl.orEmpty()
        if (binding.imageProfile.tag != photoUrl) {
            binding.imageProfile.tag = photoUrl
            Glide.with(binding.imageProfile)
                .load(photoUrl.takeIf { it.isNotBlank() })
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.imageProfile)
        }
        binding.textProfileName.text = user?.name?.ifBlank { getString(R.string.student) }
            ?: getString(R.string.student)
        binding.textProfileEmail.text = user?.email.orEmpty()
        val course = user?.course?.ifBlank { getString(R.string.default_course) }
            ?: getString(R.string.default_course)
        val year = user?.yearOfStudy?.ifBlank { getString(R.string.default_year) }
            ?: getString(R.string.default_year)
        binding.textProfileCourse.text = getString(R.string.profile_course_year_format, course, year)
        val completedCount = PlanningPolicy.completedCount(state.tasks, user?.uid.orEmpty())
        val completed = resources.getQuantityString(R.plurals.tasks_completed_count, completedCount, completedCount)
        val groups = resources.getQuantityString(R.plurals.groups_count, state.groups.size, state.groups.size)
        binding.textProfileStats.text = getString(
            R.string.profile_stats_format,
            PlanningPolicy.points(state.tasks, user?.uid.orEmpty()),
            completed,
            groups
        )
    }
}
