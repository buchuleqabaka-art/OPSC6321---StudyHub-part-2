package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import com.studyhub.app.R
import com.studyhub.app.databinding.FragmentGroupDetailBinding
import com.studyhub.app.ui.navigation.Routes

class GroupDetailFragment : HubFragment(R.layout.fragment_group_detail) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentGroupDetailBinding.bind(view)
        observe(mainViewModel.state) { state ->
            val group = state.groups.firstOrNull { it.id == state.planningGroupId }
            binding.groupHeading.text = group?.name ?: getString(R.string.planning_group)
            binding.groupCode.text = group?.let { getString(R.string.planning_invite_code, it.id) }.orEmpty()
            binding.groupSummary.text = group?.description.orEmpty()
            binding.groupTasks.isEnabled = group != null
            binding.groupSessions.isEnabled = group != null
            binding.groupChat.isEnabled = group != null
        }
        binding.groupTasks.setOnClickListener { navigator.navigate(Routes.TASKS) }
        binding.groupSessions.setOnClickListener { navigator.navigate(Routes.SESSIONS) }
        binding.groupChat.setOnClickListener {
            mainViewModel.state.value.planningGroupId?.let { mainViewModel.openGroupChat(it) }
            navigator.navigate(Routes.CHATS)
        }
        binding.groupBack.setOnClickListener { navigator.navigate(Routes.GROUPS, clearBackStack = true) }
    }
}
