package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.studyhub.app.R
import com.studyhub.app.data.model.Priority
import com.studyhub.app.data.model.Task
import com.studyhub.app.databinding.FragmentTasksBinding
import com.studyhub.app.databinding.ItemTaskBinding
import com.studyhub.app.ui.components.DialogHelper
import com.studyhub.app.util.DateUtils
import com.studyhub.app.util.PlanningPolicy

class TasksFragment : HubFragment(R.layout.fragment_tasks) {
    private var allTasks: List<Task> = emptyList()
    private var filter: Int = R.id.filter_all

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentTasksBinding.bind(view)
        binding.taskFilter.setOnCheckedChangeListener { _, checkedId ->
            filter = checkedId
            renderTasks(binding)
        }
        binding.buttonNewTask.setOnClickListener {
            DialogHelper.createTask(requireContext(), mainViewModel.state.value.groups, mainViewModel.state.value.planningGroupId, mainViewModel.state.value.profile?.name.orEmpty()) { task ->
                mainViewModel.addTask(task) { ok -> planningFeedback(binding.root, ok, R.string.planning_saved_local) }
            }
        }
        observe(mainViewModel.state) { state ->
            binding.planningScope.text = state.groups.firstOrNull { it.id == state.planningGroupId }
                ?.let { getString(R.string.planning_scope, it.name) } ?: getString(R.string.planning_all)
            allTasks = state.tasks.filter { state.planningGroupId == null || it.groupId == state.planningGroupId }
            renderTasks(binding)
        }
    }

    private fun renderTasks(binding: FragmentTasksBinding) {
        val visible = when (filter) {
            R.id.filter_pending -> allTasks.filterNot { it.completed }
            R.id.filter_completed -> allTasks.filter { it.completed }
            else -> allTasks
        }
        binding.tasksContainer.removeAllViews()
        binding.textEmptyTasks.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        visible.forEach { task ->
            val row = ItemTaskBinding.inflate(layoutInflater, binding.tasksContainer, false)
            row.taskTitle.text = task.title
            row.taskDue.text = getString(
                R.string.task_due_format,
                DateUtils.formatDate(requireContext(), task.dueDate)
            )
            val priorityLabel = when (task.priorityEnum) {
                Priority.HIGH -> getString(R.string.priority_high)
                Priority.MEDIUM -> getString(R.string.priority_medium)
                Priority.LOW -> getString(R.string.priority_low)
            }
            row.taskMeta.text = getString(
                R.string.task_priority_format,
                if (task.assignedToId == mainViewModel.state.value.profile?.uid) getString(R.string.you) else task.assignedToName.ifBlank { task.assignedToId },
                priorityLabel
            )
            val uid = mainViewModel.state.value.profile?.uid.orEmpty()
            row.taskCompleted.isEnabled = PlanningPolicy.canComplete(task, uid)
            row.taskDelete.visibility = if (task.ownerId == uid) View.VISIBLE else View.GONE
            row.taskCompleted.isChecked = task.completed
            row.taskCompleted.contentDescription = getString(
                if (task.completed) R.string.mark_task_incomplete_format else R.string.mark_task_complete_format,
                task.title
            )
            row.taskCompleted.setOnCheckedChangeListener { _, isChecked ->
                row.taskCompleted.contentDescription = getString(
                    if (isChecked) R.string.mark_task_incomplete_format else R.string.mark_task_complete_format,
                    task.title
                )
                mainViewModel.toggleTask(task)
            }
            row.taskDelete.contentDescription = getString(R.string.delete_task)
            row.taskDelete.setOnClickListener {
                mainViewModel.deleteTask(task.id)
                Snackbar.make(binding.root, R.string.task_deleted, Snackbar.LENGTH_SHORT).show()
            }
            binding.tasksContainer.addView(row.root)
        }
    }
}
