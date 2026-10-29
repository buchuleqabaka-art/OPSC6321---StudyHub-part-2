package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.studyhub.app.R
import com.studyhub.app.data.model.ChatMessage
import com.studyhub.app.databinding.FragmentChatBinding
import com.studyhub.app.databinding.ItemChatMessageBinding
import com.studyhub.app.ui.navigation.Routes
import com.studyhub.app.util.DateUtils
import com.studyhub.app.viewmodel.MainUiState

class ChatFragment : HubFragment(R.layout.fragment_chat) {
    private var currentState = MainUiState()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentChatBinding.bind(view)
        binding.buttonBack.setOnClickListener { navigator.navigate(Routes.GROUPS, clearBackStack = true) }
        binding.buttonSend.setOnClickListener {
            val message = binding.messageInput.text?.toString().orEmpty()
            if (message.isBlank()) return@setOnClickListener
            if (currentState.activeGroupId.isNullOrBlank()) {
                Snackbar.make(binding.root, "Choose a study group first", Snackbar.LENGTH_SHORT).show()
                navigator.navigate(Routes.GROUPS, clearBackStack = true)
                return@setOnClickListener
            }
            mainViewModel.sendMessage(message)
            binding.messageInput.setText("")
        }
        observe(mainViewModel.state) { state ->
            currentState = state
            val group = state.groups.firstOrNull { it.id == state.activeGroupId }
            binding.textChatTitle.text = group?.name?.takeIf { it.isNotBlank() } ?: "Group chat"
            renderMessages(binding, state.messages)
        }
    }

    private fun renderMessages(binding: FragmentChatBinding, messages: List<ChatMessage>) {
        binding.chatMessages.removeAllViews()
        messages.forEach { message ->
            val row = ItemChatMessageBinding.inflate(layoutInflater, binding.chatMessages, false)
            row.messageSender.text = message.senderName.ifBlank { "StudyHub member" }
            row.messageBody.text = message.text
            row.messageTime.text = DateUtils.formatTime(message.sentAt)
            binding.chatMessages.addView(row.root)
        }
        binding.chatScroll.post { binding.chatScroll.fullScroll(View.FOCUS_DOWN) }
    }
}
