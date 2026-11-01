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
        binding.searchInput.doAfterTextChanged {
            query = it?.toString().orEmpty()
            renderGroups(binding)
        }
        binding.groupFilter.setOnCheckedChangeListener { _, checkedId ->
            groupFilter = checkedId
            renderGroups(binding)
        }
        binding.buttonCreateGroup.setOnClickListener {
            DialogHelper.createGroup(requireContext()) { name, subject, description ->
                mainViewModel.createGroup(name, subject, description)
                Snackbar.make(binding.root, R.string.group_created, Snackbar.LENGTH_SHORT).show()
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
        binding.textEmptyGroups.setText(
            if (currentGroups.isEmpty()) R.string.empty_groups_start else R.string.empty_groups_search
        )
        filtered.forEach { group ->
            val row = ItemGroupBinding.inflate(layoutInflater, binding.groupsContainer, false)
            row.groupName.text = group.name
            row.groupSubject.text = group.subject.ifBlank { getString(R.string.generic_study_group) }
            row.groupDescription.text = group.description.ifBlank { getString(R.string.group_open_chat_hint) }
            val memberCount = resources.getQuantityString(
                R.plurals.group_member_count,
                group.memberCount,
                group.memberCount
            )
            row.groupMembers.text = getString(R.string.group_members_format, memberCount, getString(R.string.group_member_hint))
            row.root.setOnClickListener {
                mainViewModel.openGroupChat(group.id)
                navigator.navigate(Routes.CHATS)
            }
            binding.groupsContainer.addView(row.root)
        }
    }
}
