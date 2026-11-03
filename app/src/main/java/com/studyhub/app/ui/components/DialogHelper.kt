package com.studyhub.app.ui.components

import android.content.Context
import android.widget.DatePicker
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.view.View
import android.app.TimePickerDialog
import android.text.format.DateFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyhub.app.data.model.StudyGroup
import com.studyhub.app.util.PlanningPolicy
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

    fun createTask(context: Context, groups: List<StudyGroup> = emptyList(), selectedGroupId: String? = null, currentUserName: String = "", onCreate: (Task) -> Unit) {
        val binding = DialogCreateTaskBinding.inflate(android.view.LayoutInflater.from(context))
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val groupOptions = listOf<StudyGroup?>(null) + groups
        binding.taskGroup.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item,
            groupOptions.map { it?.name ?: context.getString(R.string.planning_personal) })
        var memberIds = listOf(uid)
        var memberLabels = listOf(context.getString(R.string.you))
        binding.taskGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val group = groupOptions[position]
                memberIds = group?.memberIds?.distinct().orEmpty().ifEmpty { listOf(uid) }
                memberLabels = memberIds.map { if (it == uid) context.getString(R.string.you) else context.getString(R.string.planning_member, it) }
                binding.taskAssignee.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, memberLabels)
                group?.let {
                    FirebaseFirestore.getInstance().collection("groups").document(it.id).collection("members").get()
                        .addOnSuccessListener { result ->
                            if (groupOptions[binding.taskGroup.selectedItemPosition]?.id != group.id) return@addOnSuccessListener
                            val previous = binding.taskAssignee.selectedItemPosition.coerceAtLeast(0)
                            val names = result.documents.associate { doc -> doc.id to doc.getString("name").orEmpty() }
                            memberLabels = memberIds.map { member ->
                                if (member == uid) context.getString(R.string.you)
                                else names[member]?.takeIf { name -> name.isNotBlank() }?.let { name -> "$name (${member.takeLast(6)})" }
                                    ?: context.getString(R.string.planning_member, member)
                            }
                            binding.taskAssignee.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, memberLabels)
                            binding.taskAssignee.setSelection(previous.coerceAtMost(memberIds.lastIndex))
                        }
                }
            }
        }
        binding.taskGroup.setSelection(groupOptions.indexOfFirst { it?.id == selectedGroupId }.coerceAtLeast(0))
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
                if (!PlanningPolicy.validTitle(title) || dueDate <= System.currentTimeMillis()) {
                    binding.inputTaskTitle.error = context.getString(R.string.planning_invalid_task)
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
                        groupId = groupOptions[binding.taskGroup.selectedItemPosition]?.id.orEmpty(),
                        assignedToId = memberIds[binding.taskAssignee.selectedItemPosition.coerceAtLeast(0)],
                        assignedToName = if (memberIds[binding.taskAssignee.selectedItemPosition.coerceAtLeast(0)] == uid) currentUserName else memberLabels[binding.taskAssignee.selectedItemPosition.coerceAtLeast(0)],
                        priority = priority,
                        dueDate = dueDate
                    )
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun createSession(context: Context, groups: List<StudyGroup> = emptyList(), selectedGroupId: String? = null, onCreate: (StudySession) -> Unit) {
        val binding = DialogCreateSessionBinding.inflate(android.view.LayoutInflater.from(context))
        val groupOptions = listOf<StudyGroup?>(null) + groups
        binding.sessionGroup.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item,
            groupOptions.map { it?.name ?: context.getString(R.string.planning_personal) })
        binding.sessionGroup.setSelection(groupOptions.indexOfFirst { it?.id == selectedGroupId }.coerceAtLeast(0))
        var startTime = tomorrowAt(14)
        var endTime = startTime + 2 * 60 * 60 * 1000L
        fun renderSessionDate() {
            binding.buttonStartTime.text = context.getString(R.string.planning_start_at, DateUtils.formatTime(context, startTime))
            binding.buttonEndTime.text = context.getString(R.string.planning_end_at, DateUtils.formatTime(context, endTime))
            binding.buttonPickSessionDate.text = context.getString(
                R.string.session_date_time_format,
                DateUtils.formatDate(context, startTime),
                DateUtils.formatTime(context, startTime)
            )
        }
        renderSessionDate()
        binding.buttonPickSessionDate.setOnClickListener {
            showDatePicker(context, startTime) { picked ->
                val old = Calendar.getInstance().apply { timeInMillis = startTime }
                val oldEnd = Calendar.getInstance().apply { timeInMillis = endTime }
                startTime = Calendar.getInstance().apply { timeInMillis = picked; set(Calendar.HOUR_OF_DAY, old.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, old.get(Calendar.MINUTE)) }.timeInMillis
                endTime = Calendar.getInstance().apply { timeInMillis = picked; set(Calendar.HOUR_OF_DAY, oldEnd.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, oldEnd.get(Calendar.MINUTE)) }.timeInMillis
                renderSessionDate()
            }
        }
        fun pickTime(initial: Long, onPicked: (Long) -> Unit) {
            val time = Calendar.getInstance().apply { timeInMillis = initial }
            TimePickerDialog(context, { _, hour, minute ->
                time.set(Calendar.HOUR_OF_DAY, hour)
                time.set(Calendar.MINUTE, minute)
                time.set(Calendar.SECOND, 0)
                time.set(Calendar.MILLISECOND, 0)
                onPicked(time.timeInMillis)
                renderSessionDate()
            }, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), DateFormat.is24HourFormat(context)).show()
        }
        binding.buttonStartTime.setOnClickListener { pickTime(startTime) { startTime = it } }
        binding.buttonEndTime.setOnClickListener { pickTime(endTime) { endTime = it } }
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
                if (!PlanningPolicy.validSession(title, startTime, endTime, System.currentTimeMillis())) {
                    binding.sessionError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                onCreate(
                    StudySession(
                        groupId = groupOptions[binding.sessionGroup.selectedItemPosition]?.id.orEmpty(),
                        title = title,
                        description = binding.inputSessionDescription.text?.toString()?.trim().orEmpty(),
                        location = binding.inputSessionLocation.text?.toString()?.trim().orEmpty(),
                        startTime = startTime,
                        endTime = endTime
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
