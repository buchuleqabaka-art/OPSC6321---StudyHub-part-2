package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.studyhub.app.R
import com.studyhub.app.data.model.Task
import com.studyhub.app.databinding.FragmentTasksBinding
import com.studyhub.app.databinding.ItemTaskBinding
import com.studyhub.app.ui.components.DialogHelper

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
            DialogHelper.createTask(requireContext()) { task ->
                mainViewModel.addTask(task)
                Snackbar.make(binding.root, "Task added", Snackbar.LENGTH_SHORT).show()
            }
        }
        observe(mainViewModel.state) { state ->
            allTasks = state.tasks
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
            row.taskDue.text = "Due ${com.studyhub.app.util.DateUtils.formatDate(task.dueDate)}"
            row.taskMeta.text = "${task.assignedToName.ifBlank { "You" }} · ${task.priorityEnum.name.lowercase().replaceFirstChar { it.uppercase() }} priority"
            row.taskCompleted.isChecked = task.completed
            row.taskCompleted.setOnCheckedChangeListener { _, _ ->
                mainViewModel.toggleTask(task)
            }
            row.taskDelete.setOnClickListener {
                mainViewModel.deleteTask(task.id)
                Snackbar.make(binding.root, "Task deleted", Snackbar.LENGTH_SHORT).show()
            }
            binding.tasksContainer.addView(row.root)
        }
    }
}
