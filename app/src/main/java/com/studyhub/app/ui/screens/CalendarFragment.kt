package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.studyhub.app.R
import com.studyhub.app.databinding.FragmentCalendarBinding
import com.studyhub.app.databinding.ItemPreviewBinding
import com.studyhub.app.ui.navigation.Routes
import com.studyhub.app.util.DateUtils
import com.studyhub.app.viewmodel.MainUiState
import java.util.Calendar

class CalendarFragment : HubFragment(R.layout.fragment_calendar) {
    private var selectedDay = System.currentTimeMillis()
    private var currentState = MainUiState()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentCalendarBinding.bind(view)
        binding.buttonBack.setOnClickListener { navigator.navigate(Routes.HOME, clearBackStack = true) }
        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            selectedDay = Calendar.getInstance().apply { set(year, month, day, 12, 0, 0) }.timeInMillis
            renderAgenda(binding)
        }
        observe(mainViewModel.state) { state ->
            currentState = state
            renderAgenda(binding)
        }
    }

    private fun renderAgenda(binding: FragmentCalendarBinding) {
        binding.textAgendaTitle.text = "Agenda · ${DateUtils.formatDate(selectedDay)}"
        val tasks = currentState.tasks.filter { DateUtils.isSameDay(it.dueDate, selectedDay) }
        val sessions = currentState.sessions.filter { DateUtils.isSameDay(it.startTime, selectedDay) }
        binding.agendaContainer.removeAllViews()
        binding.textEmptyAgenda.visibility = if (tasks.isEmpty() && sessions.isEmpty()) View.VISIBLE else View.GONE
        tasks.forEach { addAgendaRow(binding.agendaContainer, it.title, "Task · Due ${DateUtils.formatDate(it.dueDate)}") }
        sessions.forEach {
            addAgendaRow(binding.agendaContainer, it.title, "Session · ${DateUtils.formatRange(it.startTime, it.endTime)} · ${it.location.ifBlank { "Location TBC" }}")
        }
    }

    private fun addAgendaRow(container: LinearLayout, title: String, detail: String) {
        val row = ItemPreviewBinding.inflate(layoutInflater, container, false)
        row.previewTitle.text = title
        row.previewDetail.text = detail
        container.addView(row.root)
    }
}
