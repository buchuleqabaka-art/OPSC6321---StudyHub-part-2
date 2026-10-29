package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.studyhub.app.R
import com.studyhub.app.data.model.StudySession
import com.studyhub.app.databinding.FragmentSessionsBinding
import com.studyhub.app.databinding.ItemSessionBinding
import com.studyhub.app.ui.components.DialogHelper
import com.studyhub.app.util.DateUtils
import com.studyhub.app.viewmodel.MainUiState

class SessionsFragment : HubFragment(R.layout.fragment_sessions) {
    private var currentState = MainUiState()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSessionsBinding.bind(view)
        binding.buttonScheduleSession.setOnClickListener {
            DialogHelper.createSession(requireContext()) { session ->
                mainViewModel.addSession(session)
                Snackbar.make(binding.root, "Session scheduled", Snackbar.LENGTH_SHORT).show()
            }
        }
        observe(mainViewModel.state) { state ->
            currentState = state
            renderSessions(binding)
        }
    }

    private fun renderSessions(binding: FragmentSessionsBinding) {
        val upcoming = currentState.sessions.filter { it.startTime >= System.currentTimeMillis() }
        binding.sessionsContainer.removeAllViews()
        binding.textEmptySessions.visibility = if (upcoming.isEmpty()) View.VISIBLE else View.GONE
        upcoming.forEach { session ->
            val row = ItemSessionBinding.inflate(layoutInflater, binding.sessionsContainer, false)
            row.sessionTitle.text = session.title
            row.sessionDescription.text = session.description.ifBlank { "Study with your group" }
            row.sessionDate.text = "${DateUtils.formatDate(session.startTime)} · ${DateUtils.formatRange(session.startTime, session.endTime)}"
            row.sessionLocation.text = "${session.location.ifBlank { "Location to be confirmed" }} · ${session.goingCount} going"
            val uid = currentState.profile?.uid
            val attending = !uid.isNullOrBlank() && session.attendeeIds.contains(uid)
            row.sessionAttend.text = if (attending) "I'm going ✓" else "I'm going"
            row.sessionAttend.setOnClickListener { mainViewModel.toggleAttendance(session.id) }
            binding.sessionsContainer.addView(row.root)
        }
    }
}
