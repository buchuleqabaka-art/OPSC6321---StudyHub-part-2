package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.viewModels
import com.studyhub.app.R
import com.studyhub.app.engagement.*
import com.studyhub.app.util.DateUtils

class AttendanceFragment : HubFragment(R.layout.fragment_engagement) {
    private val model: EngagementViewModel by viewModels()
    private var recent = false
    private var groupId: String? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recent = savedInstanceState?.getBoolean("recent") ?: false
        groupId = savedInstanceState?.getString("group") ?: mainViewModel.state.value.planningGroupId
        observe(mainViewModel.state) { render(view) }
        observe(model.state) { render(view) }
    }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putBoolean("recent", recent); outState.putString("group", groupId) }
    private fun render(view: View) {
        val box = view.findViewById<LinearLayout>(R.id.engagement_content)
        val state = model.state.value; val main = mainViewModel.state.value
        box.removeAllViews(); box.label(getString(R.string.attendance_title), 26f)
        box.label(getString(R.string.attendance_note))
        if (state.uid == null || main.profile?.uid != state.uid) { box.label(getString(R.string.engagement_loading)); return }
        if (state.failed) { box.label(getString(R.string.engagement_failed)); box.action(getString(R.string.engagement_refresh)) { model.refresh() } }
        if (state.busy) box.addView(ProgressBar(requireContext()))
        val groups = listOf(null) + main.groups.map { it.id }
        if (groupId !in groups) groupId = null
        box.addView(Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf(getString(R.string.attendance_all_groups)) + main.groups.map { it.name })
            setSelection(groups.indexOf(groupId).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, selected: View?, position: Int, id: Long) {
                    if (groupId != groups[position]) { groupId = groups[position]; render(view) }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        })
        box.addView(CheckBox(requireContext()).apply { text = getString(R.string.attendance_recent); isChecked = recent
            setOnCheckedChangeListener { _, checked -> recent = checked; render(view) } })
        if (!state.attendanceLoaded) { box.label(getString(R.string.engagement_loading)); return }
        if (state.cache) box.label(getString(R.string.engagement_cached))
        val now = System.currentTimeMillis()
        val sessions = main.sessions.distinctBy { it.id }.filter { it.endTime <= now &&
            (groupId == null || it.groupId == groupId) && (!recent || it.endTime >= now - 30L * 86400000) }.sortedByDescending { it.endTime }
        val summary = AttendancePolicy.summarize(sessions, state.records, now)
        box.label(summary.percent?.let { getString(R.string.attendance_percentage, it) } ?: getString(R.string.attendance_no_ratio), 22f)
        box.label(getString(R.string.attendance_ratio, summary.attended, summary.absent))
        box.bar(summary.attended, summary.recorded, getString(R.string.attendance_count, getString(R.string.attendance_present), summary.attended), requireContext().getColor(R.color.lilac_primary_dark))
        box.bar(summary.absent, summary.recorded, getString(R.string.attendance_count, getString(R.string.attendance_absent), summary.absent), requireContext().getColor(R.color.ink_muted))
        box.label(getString(R.string.attendance_count, getString(R.string.attendance_unknown), summary.unrecorded))
        if (sessions.isEmpty()) box.label(getString(R.string.attendance_empty))
        sessions.forEach { session ->
            val card = box.section()
            card.label(session.title, 20f)
            card.label(DateUtils.formatDate(requireContext(), session.startTime))
            val status = when (state.records[session.id]) { "attended" -> R.string.attendance_present; "absent" -> R.string.attendance_absent; else -> R.string.attendance_unknown }
            card.label(getString(status))
            card.action(getString(R.string.attendance_update), !state.busy) {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(session.title)
                    .setItems(arrayOf(getString(R.string.attendance_present), getString(R.string.attendance_absent), getString(R.string.attendance_clear))) { _, index ->
                        model.mark(session.id, when(index) { 0 -> "attended"; 1 -> "absent"; else -> null })
                    }.setNegativeButton(android.R.string.cancel, null).show()
            }
        }
    }
}
