package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
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
        binding.tvGreeting.text = "Good day, ${state.profile?.name?.substringBefore(' ')?.ifBlank { "Student" } ?: "Student"}"
        binding.tvGroupCount.text = state.groups.size.toString()
        binding.tvTaskCount.text = state.pendingTasks.size.toString()
        binding.tvSessionCount.text = state.sessions.count { it.startTime >= System.currentTimeMillis() }.toString()
        binding.tvQuote.text = state.quote?.content ?: "Small consistent effort beats one long night before the deadline."
        binding.tvQuoteAuthor.text = "— ${state.quote?.author?.ifBlank { "StudyHub" } ?: "StudyHub"}"
        renderPreviews(binding.containerUpcomingTasks, state.upcomingTasks) { task ->
            task.title to "${DateUtils.formatDate(task.dueDate)} · ${DateUtils.dueInLabel(task.dueDate)}"
        }
        val next = state.nextSession
        binding.containerUpcomingSessions.removeAllViews()
        if (next != null) {
            val row = ItemPreviewBinding.inflate(layoutInflater, binding.containerUpcomingSessions, false)
            row.previewTitle.text = next.title
            row.previewDetail.text = "${DateUtils.formatDate(next.startTime)} · ${DateUtils.formatRange(next.startTime, next.endTime)} · ${next.location.ifBlank { "Location TBC" }}"
            binding.containerUpcomingSessions.addView(row.root)
            row.root.setOnClickListener { navigator.navigate(Routes.SESSIONS, clearBackStack = true) }
        } else {
            addEmptyText(binding.containerUpcomingSessions, "No sessions scheduled yet.")
        }
    }

    private fun renderPreviews(
        container: LinearLayout,
        tasks: List<Task>,
        text: (Task) -> Pair<String, String>
    ) {
        container.removeAllViews()
        if (tasks.isEmpty()) {
            addEmptyText(container, "You're all caught up. Add a task when you're ready.")
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
