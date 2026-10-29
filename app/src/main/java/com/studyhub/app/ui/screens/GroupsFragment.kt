package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.snackbar.Snackbar
import com.studyhub.app.R
import com.studyhub.app.data.model.StudyGroup
import com.studyhub.app.databinding.FragmentGroupsBinding
import com.studyhub.app.databinding.ItemGroupBinding
import com.studyhub.app.ui.components.DialogHelper
import com.studyhub.app.ui.navigation.Routes

class GroupsFragment : HubFragment(R.layout.fragment_groups) {
    private var currentGroups: List<StudyGroup> = emptyList()
    private var currentUid: String = ""
    private var query: String = ""
    private var groupFilter: Int = R.id.group_filter_all

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentGroupsBinding.bind(view)
        binding.searchInput.doAfterTextChanged { query = it?.toString().orEmpty(); renderGroups(binding) }
        binding.groupFilter.setOnCheckedChangeListener { _, checkedId ->
            groupFilter = checkedId
            renderGroups(binding)
        }
        binding.buttonCreateGroup.setOnClickListener {
            DialogHelper.createGroup(requireContext()) { name, subject, description ->
                mainViewModel.createGroup(name, subject, description)
                Snackbar.make(binding.root, "Study group created", Snackbar.LENGTH_SHORT).show()
            }
        }
        observe(mainViewModel.state) { state ->
            currentGroups = state.groups
            currentUid = state.profile?.uid.orEmpty()
            renderGroups(binding)
        }
    }

    private fun renderGroups(binding: FragmentGroupsBinding) {
        val filtered = currentGroups
            .filter { group ->
                when (groupFilter) {
                    R.id.group_filter_owned -> group.ownerId == currentUid
                    R.id.group_filter_joined -> group.ownerId != currentUid
                    else -> true
                }
            }
            .filter { group ->
                val text = query.trim()
                text.isBlank() || group.name.contains(text, true) || group.subject.contains(text, true)
            }
        binding.groupsContainer.removeAllViews()
        binding.textEmptyGroups.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.textEmptyGroups.text = if (currentGroups.isEmpty()) {
            "No groups yet. Create one to get started."
        } else {
            "No groups match your search."
        }
        filtered.forEach { group ->
            val row = ItemGroupBinding.inflate(layoutInflater, binding.groupsContainer, false)
            row.groupName.text = group.name
            row.groupSubject.text = group.subject.ifBlank { "Study group" }
            row.groupDescription.text = group.description.ifBlank { "Tap to open your group chat." }
            row.groupMembers.text = "${group.memberCount} ${if (group.memberCount == 1) "member" else "members"} · Tap to open chat"
            row.root.setOnClickListener {
                mainViewModel.openGroupChat(group.id)
                navigator.navigate(Routes.CHATS)
            }
            binding.groupsContainer.addView(row.root)
        }
    }
}
