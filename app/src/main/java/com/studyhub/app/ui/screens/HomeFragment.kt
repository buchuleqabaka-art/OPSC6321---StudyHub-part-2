package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.studyhub.app.R
import com.studyhub.app.data.model.Task
import com.studyhub.app.databinding.FragmentHomeBinding
import com.studyhub.app.databinding.ItemPreviewBinding
import com.studyhub.app.ui.navigation.Routes
import com.studyhub.app.util.DateUtils
import com.studyhub.app.viewmodel.MainUiState

class HomeFragment : HubFragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel.selectPlanningGroup(null)
        _binding = FragmentHomeBinding.bind(view)
        binding.buttonHomeGroups.setOnClickListener { navigator.navigate(Routes.GROUPS, clearBackStack = true) }
        binding.buttonHomeTasks.setOnClickListener { navigator.navigate(Routes.TASKS, clearBackStack = true) }
        binding.linkAllTasks.setOnClickListener { navigator.navigate(Routes.TASKS, clearBackStack = true) }
        binding.buttonHomeCalendar.setOnClickListener { navigator.navigate(Routes.CALENDAR) }
        binding.buttonHomeSessions.setOnClickListener { navigator.navigate(Routes.SESSIONS, clearBackStack = true) }
        binding.linkAllSessions.setOnClickListener { navigator.navigate(Routes.SESSIONS, clearBackStack = true) }
        binding.buttonHomeChats.setOnClickListener {
            val groupId = mainViewModel.state.value.groups.firstOrNull()?.id
            if (groupId.isNullOrBlank()) {
                navigator.navigate(Routes.GROUPS, clearBackStack = true)
            } else {
                mainViewModel.openGroupChat(groupId)
                navigator.navigate(Routes.CHATS)
            }
        }
        observe(mainViewModel.state) { render(it) }
    }

    private fun render(state: MainUiState) {
        val studentName = state.profile?.name?.substringBefore(' ')?.ifBlank { null }
            ?: getString(R.string.student)
        binding.tvGreeting.text = getString(R.string.home_greeting, studentName)
        binding.tvGroupCount.text = state.groups.size.toString()
        binding.tvTaskCount.text = state.pendingTasks.size.toString()
        binding.tvSessionCount.text = state.sessions.count {
            it.startTime >= System.currentTimeMillis()
        }.toString()
        binding.tvQuote.text = state.quote?.content?.takeIf { it.isNotBlank() }
            ?: getString(R.string.quote_fallback)
        val quoteAuthor = state.quote?.author?.ifBlank { null } ?: getString(R.string.app_name)
        binding.tvQuoteAuthor.text = getString(R.string.quote_author_format, quoteAuthor)
        binding.textSyncStatus.text = if (state.pendingTaskSyncCount == 0) {
            getString(R.string.sync_status_up_to_date)
        } else {
            resources.getQuantityString(
                R.plurals.sync_changes_waiting,
                state.pendingTaskSyncCount,
                state.pendingTaskSyncCount
            )
        }

        renderPreviews(binding.containerUpcomingTasks, state.upcomingTasks) { task ->
            getString(
                R.string.task_preview_format,
                DateUtils.formatDate(requireContext(), task.dueDate),
                DateUtils.dueInLabel(requireContext(), task.dueDate)
            ).let { task.title to it }
        }

        binding.containerUpcomingSessions.removeAllViews()
        val next = state.nextSession
        if (next != null) {
            val row = ItemPreviewBinding.inflate(layoutInflater, binding.containerUpcomingSessions, false)
            row.previewTitle.text = next.title
            val location = next.location.ifBlank { getString(R.string.location_tbc_short) }
            row.previewDetail.text = getString(
                R.string.session_preview_format,
                DateUtils.formatDate(requireContext(), next.startTime),
                DateUtils.formatRange(requireContext(), next.startTime, next.endTime),
                location
            )
            binding.containerUpcomingSessions.addView(row.root)
            row.root.setOnClickListener { navigator.navigate(Routes.SESSIONS, clearBackStack = true) }
        } else {
            addEmptyText(binding.containerUpcomingSessions, getString(R.string.empty_upcoming_sessions))
        }
    }

    private fun renderPreviews(
        container: LinearLayout,
        tasks: List<Task>,
        text: (Task) -> Pair<String, String>
    ) {
        container.removeAllViews()
        if (tasks.isEmpty()) {
            addEmptyText(container, getString(R.string.empty_tasks_caught_up))
            return
        }
        tasks.take(3).forEach { task ->
            val row = ItemPreviewBinding.inflate(layoutInflater, container, false)
            val (title, detail) = text(task)
            row.previewTitle.text = title
            row.previewDetail.text = detail
            container.addView(row.root)
            row.root.setOnClickListener { navigator.navigate(Routes.TASKS, clearBackStack = true) }
        }
    }

    private fun addEmptyText(container: LinearLayout, text: String) {
        val empty = android.widget.TextView(requireContext()).apply {
            setTextColor(requireContext().getColor(R.color.ink_muted))
            textSize = 14f
            setPadding(4, 16, 4, 12)
            this.text = text
        }
        container.addView(empty)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
