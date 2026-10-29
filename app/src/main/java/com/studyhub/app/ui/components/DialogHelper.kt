package com.studyhub.app.ui.components

import android.content.Context
import android.widget.DatePicker
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyhub.app.R
import com.studyhub.app.data.model.StudySession
import com.studyhub.app.data.model.Task
import com.studyhub.app.databinding.DialogCreateGroupBinding
import com.studyhub.app.databinding.DialogCreateSessionBinding
import com.studyhub.app.databinding.DialogCreateTaskBinding
import com.studyhub.app.databinding.DialogEditProfileBinding
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.util.DateUtils
import java.util.Calendar

object DialogHelper {
    fun createGroup(context: Context, onCreate: (String, String, String) -> Unit) {
        val binding = DialogCreateGroupBinding.inflate(android.view.LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Create a study group")
            .setView(binding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create", null)
            .create()
        dialog.setOnShowListener {
            val createButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            createButton.isEnabled = !binding.inputGroupName.text.isNullOrBlank()
            binding.inputGroupName.doAfterTextChanged { createButton.isEnabled = !it.isNullOrBlank() }
            createButton.setOnClickListener {
                val name = binding.inputGroupName.text?.toString()?.trim().orEmpty()
                if (name.isBlank()) {
                    binding.inputGroupName.error = "Enter a group name"
                    return@setOnClickListener
                }
                onCreate(
                    name,
                    binding.inputGroupSubject.text?.toString()?.trim().orEmpty(),
                    binding.inputGroupDescription.text?.toString()?.trim().orEmpty()
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun createTask(context: Context, onCreate: (Task) -> Unit) {
        val binding = DialogCreateTaskBinding.inflate(android.view.LayoutInflater.from(context))
        var dueDate = tomorrowAt(9)
        binding.buttonPickTaskDate.text = "Due ${DateUtils.formatDate(dueDate)}"
        binding.buttonPickTaskDate.setOnClickListener {
            showDatePicker(context, dueDate) { picked ->
                dueDate = atHour(picked, 9)
                binding.buttonPickTaskDate.text = "Due ${DateUtils.formatDate(dueDate)}"
            }
        }
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Add a task")
            .setView(binding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add task", null)
            .create()
        dialog.setOnShowListener {
            val addButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            addButton.isEnabled = !binding.inputTaskTitle.text.isNullOrBlank()
            binding.inputTaskTitle.doAfterTextChanged { addButton.isEnabled = !it.isNullOrBlank() }
            addButton.setOnClickListener {
                val title = binding.inputTaskTitle.text?.toString()?.trim().orEmpty()
                if (title.isBlank()) {
                    binding.inputTaskTitle.error = "Enter a task title"
                    return@setOnClickListener
                }
                val priority = when (binding.taskPriorityGroup.checkedRadioButtonId) {
                    R.id.priority_high -> "HIGH"
                    R.id.priority_low -> "LOW"
                    else -> "MEDIUM"
                }
                onCreate(
                    Task(
                        title = title,
                        assignedToName = binding.inputTaskAssignee.text?.toString()?.trim().orEmpty().ifBlank { "You" },
                        priority = priority,
                        dueDate = dueDate
                    )
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun createSession(context: Context, onCreate: (StudySession) -> Unit) {
        val binding = DialogCreateSessionBinding.inflate(android.view.LayoutInflater.from(context))
        var startTime = tomorrowAt(14)
        binding.buttonPickSessionDate.text = "${DateUtils.formatDate(startTime)} · ${DateUtils.formatTime(startTime)}"
        binding.buttonPickSessionDate.setOnClickListener {
            showDatePicker(context, startTime) { picked ->
                startTime = atHour(picked, 14)
                binding.buttonPickSessionDate.text = "${DateUtils.formatDate(startTime)} · ${DateUtils.formatTime(startTime)}"
            }
        }
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Schedule a session")
            .setView(binding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Schedule", null)
            .create()
        dialog.setOnShowListener {
            val scheduleButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            scheduleButton.isEnabled = !binding.inputSessionTitle.text.isNullOrBlank()
            binding.inputSessionTitle.doAfterTextChanged { scheduleButton.isEnabled = !it.isNullOrBlank() }
            scheduleButton.setOnClickListener {
                val title = binding.inputSessionTitle.text?.toString()?.trim().orEmpty()
                if (title.isBlank()) {
                    binding.inputSessionTitle.error = "Enter a session title"
                    return@setOnClickListener
                }
                onCreate(
                    StudySession(
                        title = title,
                        description = binding.inputSessionDescription.text?.toString()?.trim().orEmpty(),
                        location = binding.inputSessionLocation.text?.toString()?.trim().orEmpty(),
                        startTime = startTime,
                        endTime = startTime + 2 * 60 * 60 * 1000L
                    )
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun editProfile(context: Context, profile: UserProfile, onSave: (String, String, String) -> Unit) {
        val binding = DialogEditProfileBinding.inflate(android.view.LayoutInflater.from(context))
        binding.inputProfileName.setText(profile.name)
        binding.inputProfileCourse.setText(profile.course)
        binding.inputProfileEmail.setText(profile.email)
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Edit profile")
            .setView(binding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = binding.inputProfileName.text?.toString()?.trim().orEmpty()
                val course = binding.inputProfileCourse.text?.toString()?.trim().orEmpty()
                val email = binding.inputProfileEmail.text?.toString()?.trim().orEmpty()
                if (name.isBlank() || email.isBlank()) {
                    if (name.isBlank()) binding.inputProfileName.error = "Name is required"
                    if (email.isBlank()) binding.inputProfileEmail.error = "Email is required"
                    return@setOnClickListener
                }
                onSave(name, course, email)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showDatePicker(context: Context, initialTime: Long, onPicked: (Long) -> Unit) {
        val initial = Calendar.getInstance().apply { timeInMillis = initialTime }
        android.app.DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, day: Int ->
                val selected = Calendar.getInstance().apply { set(year, month, day, 12, 0, 0); set(Calendar.MILLISECOND, 0) }
                onPicked(selected.timeInMillis)
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun tomorrowAt(hour: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun atHour(timestamp: Long, hour: Int): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
