package com.studyhub.app.ui.screens

import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.*
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyhub.app.R
import com.studyhub.app.engagement.*

class PollsFragment : HubFragment(R.layout.fragment_engagement) {
    private val model: EngagementViewModel by viewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(mainViewModel.state) { model.bindGroup(it.activeGroupId); render(view) }
        observe(model.state) { render(view) }
    }
    private fun render(view: View) {
        val box = view.findViewById<LinearLayout>(R.id.engagement_content); val state = model.state.value
        box.removeAllViews(); box.label(getString(R.string.poll_title), 26f); box.label(getString(R.string.poll_note))
        val groupId = mainViewModel.state.value.activeGroupId
        val group = mainViewModel.state.value.groups.firstOrNull { it.id == groupId }
        if (group == null || state.uid == null) { box.label(getString(R.string.choose_group_first)); return }
        box.label(group.name, 22f)
        if (state.failed) { box.label(getString(R.string.engagement_failed)); box.action(getString(R.string.engagement_refresh)) { model.refresh() } }
        if (state.busy) box.addView(ProgressBar(requireContext()))
        box.action(getString(R.string.poll_create), !state.busy) { createPoll() }
        if (!state.pollsLoaded) { box.label(getString(R.string.engagement_loading)); return }
        if (state.polls.isEmpty()) box.label(getString(R.string.poll_empty))
        state.polls.forEach { poll ->
            box.label(poll.question, 22f)
            box.label(getString(if (poll.closed) R.string.poll_closed else R.string.poll_open))
            box.label(getString(R.string.poll_total, poll.votes.size))
            val selected = poll.votes[state.uid]
            val counts = poll.counts
            poll.options.forEachIndexed { index, option ->
                box.bar(counts[index], poll.votes.size, getString(R.string.poll_result, option, counts[index]), requireContext().getColor(R.color.lilac_primary_dark))
                box.action(getString(if (selected == index) R.string.poll_selected else R.string.poll_vote, option), !state.busy && !poll.closed && selected != index) { model.vote(poll.id, index) }
            }
            if (!poll.closed && poll.ownerId == state.uid) box.action(getString(R.string.poll_close), !state.busy) {
                MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.poll_close_confirm).setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.poll_close) { _, _ -> model.closePoll(poll.id) }.show()
            }
        }
    }
    private fun createPoll() {
        val form = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 8) }
        val question = EditText(requireContext()).apply { hint = getString(R.string.poll_question); filters = arrayOf(InputFilter.LengthFilter(200)) }; form.addView(question)
        val options = (1..4).map { i -> EditText(requireContext()).apply { hint = getString(R.string.poll_option, i); filters = arrayOf(InputFilter.LengthFilter(100)); form.addView(this) } }
        val scroll = ScrollView(requireContext()).apply { addView(form) }
        val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.poll_create).setView(scroll)
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.poll_create, null).create()
        dialog.setOnShowListener { dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val choices = options.map { it.text.toString().trim() }.filter { it.isNotEmpty() }
            if (!PollPolicy.valid(question.text.toString(), choices)) { question.error = getString(R.string.poll_invalid); return@setOnClickListener }
            model.create(question.text.toString(), choices); dialog.dismiss()
        } }; dialog.show()
    }
}
