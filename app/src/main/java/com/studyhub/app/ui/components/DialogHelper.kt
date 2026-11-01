package com.studyhub.app.ui.components

import android.content.Context
import android.widget.DatePicker
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyhub.app.R
import com.studyhub.app.data.model.StudySession
import com.studyhub.app.data.model.Task
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.databinding.DialogCreateGroupBinding
import com.studyhub.app.databinding.DialogCreateSessionBinding
import com.studyhub.app.databinding.DialogCreateTaskBinding
import com.studyhub.app.databinding.DialogEditProfileBinding
import com.studyhub.app.util.DateUtils
import com.studyhub.app.util.Validators
import java.util.Calendar

object DialogHelper {
    fun createGroup(context: Context, onCreate: (String, String, String) -> Unit) {
        val binding = DialogCreateGroupBinding.inflate(android.view.LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.group_dialog_title)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.create, null)
            .create()
        dialog.setOnShowListener {
            val createButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            createButton.isEnabled = !binding.inputGroupName.text.isNullOrBlank()
            binding.inputGroupName.doAfterTextChanged { createButton.isEnabled = !it.isNullOrBlank() }
            createButton.setOnClickListener {
                val name = binding.inputGroupName.text?.toString()?.trim().orEmpty()
                if (name.isBlank()) {
                    binding.inputGroupName.error = context.getString(R.string.error_group_name)
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
        fun renderDueDate() {
            binding.buttonPickTaskDate.text = context.getString(
                R.string.task_due_format,
                DateUtils.formatDate(context, dueDate)
            )
        }
        renderDueDate()
        binding.buttonPickTaskDate.setOnClickListener {
            showDatePicker(context, dueDate) { picked ->
                dueDate = atHour(picked, 9)
                renderDueDate()
            }
        }
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.task_dialog_title)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.add_task, null)
            .create()
        dialog.setOnShowListener {
            val addButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            addButton.isEnabled = !binding.inputTaskTitle.text.isNullOrBlank()
            binding.inputTaskTitle.doAfterTextChanged { addButton.isEnabled = !it.isNullOrBlank() }
            addButton.setOnClickListener {
                val title = binding.inputTaskTitle.text?.toString()?.trim().orEmpty()
                if (title.isBlank()) {
                    binding.inputTaskTitle.error = context.getString(R.string.error_task_title)
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
                        assignedToName = binding.inputTaskAssignee.text?.toString()?.trim().orEmpty()
                            .ifBlank { context.getString(R.string.you) },
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
        fun renderSessionDate() {
            binding.buttonPickSessionDate.text = context.getString(
                R.string.session_date_time_format,
                DateUtils.formatDate(context, startTime),
                DateUtils.formatTime(context, startTime)
            )
        }
        renderSessionDate()
        binding.buttonPickSessionDate.setOnClickListener {
            showDatePicker(context, startTime) { picked ->
                startTime = atHour(picked, 14)
                renderSessionDate()
            }
        }
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.schedule_session_title)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.schedule, null)
            .create()
        dialog.setOnShowListener {
            val scheduleButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            scheduleButton.isEnabled = !binding.inputSessionTitle.text.isNullOrBlank()
            binding.inputSessionTitle.doAfterTextChanged { scheduleButton.isEnabled = !it.isNullOrBlank() }
            scheduleButton.setOnClickListener {
                val title = binding.inputSessionTitle.text?.toString()?.trim().orEmpty()
                if (title.isBlank()) {
                    binding.inputSessionTitle.error = context.getString(R.string.error_session_title)
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
            .setTitle(R.string.edit_profile_title)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = binding.inputProfileName.text?.toString()?.trim().orEmpty()
                val course = binding.inputProfileCourse.text?.toString()?.trim().orEmpty()
                val email = binding.inputProfileEmail.text?.toString()?.trim().orEmpty()
                var valid = true
                if (name.isBlank()) {
                    binding.inputProfileName.error = context.getString(R.string.error_name_required)
                    valid = false
                }
                if (email.isBlank()) {
                    binding.inputProfileEmail.error = context.getString(R.string.error_email_required)
                    valid = false
                } else if (!Validators.isValidEmail(email)) {
                    binding.inputProfileEmail.error = context.getString(R.string.error_email_invalid)
                    valid = false
                }
                if (!valid) return@setOnClickListener
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
                val selected = Calendar.getInstance().apply {
                    set(year, month, day, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
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
