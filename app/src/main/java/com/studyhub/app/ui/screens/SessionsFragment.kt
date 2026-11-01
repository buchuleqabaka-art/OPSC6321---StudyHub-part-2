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
                Snackbar.make(binding.root, R.string.session_scheduled, Snackbar.LENGTH_SHORT).show()
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
            row.sessionDescription.text = session.description.ifBlank { getString(R.string.study_with_group) }
            row.sessionDate.text = getString(
                R.string.session_date_time_format,
                DateUtils.formatDate(requireContext(), session.startTime),
                DateUtils.formatRange(requireContext(), session.startTime, session.endTime)
            )
            val location = session.location.ifBlank { getString(R.string.location_tbc) }
            val attendance = resources.getQuantityString(
                R.plurals.session_going_count,
                session.goingCount,
                session.goingCount
            )
            row.sessionLocation.text = getString(R.string.session_location_attendance_format, location, attendance)
            val uid = currentState.profile?.uid
            val attending = !uid.isNullOrBlank() && session.attendeeIds.contains(uid)
            row.sessionAttend.setText(if (attending) R.string.attending else R.string.not_attending)
            row.sessionAttend.setOnClickListener { mainViewModel.toggleAttendance(session.id) }
            binding.sessionsContainer.addView(row.root)
        }
    }
}
