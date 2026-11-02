package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.snackbar.Snackbar
import com.studyhub.app.R
import com.studyhub.app.data.model.ChatMessage
import com.studyhub.app.databinding.FragmentChatBinding
import com.studyhub.app.databinding.ItemChatMessageBinding
import com.studyhub.app.ui.navigation.Routes
import com.studyhub.app.util.DateUtils
import com.studyhub.app.viewmodel.MainUiState
import com.studyhub.app.viewmodel.ChatComposer

class ChatFragment : HubFragment(R.layout.fragment_chat) {
    private var currentState = MainUiState()
    private var renderedMessages: List<ChatMessage>? = null
    private var renderedGroupId: String? = null
    private var updatingDraft = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentChatBinding.bind(view)
        renderedMessages = null
        renderedGroupId = null
        binding.messageInput.doAfterTextChanged {
            if (!updatingDraft) mainViewModel.editChatDraft(it?.toString().orEmpty())
        }
        binding.chatLoadRetry.setOnClickListener {
            currentState.activeGroupId?.let(mainViewModel::openGroupChat)
        }
        binding.buttonBack.setOnClickListener { navigator.navigate(Routes.GROUPS, clearBackStack = true) }
        binding.buttonSend.setOnClickListener {
            val message = binding.messageInput.text?.toString().orEmpty()
            if (message.isBlank()) return@setOnClickListener
            if (currentState.activeGroupId.isNullOrBlank()) {
                Snackbar.make(binding.root, R.string.choose_group_first, Snackbar.LENGTH_SHORT).show()
                navigator.navigate(Routes.GROUPS, clearBackStack = true)
                return@setOnClickListener
            }
            mainViewModel.editChatDraft(message)
            mainViewModel.sendMessage()
        }
        observe(mainViewModel.state) { state ->
            currentState = state
            val group = state.groups.firstOrNull { it.id == state.activeGroupId }
            binding.textChatTitle.text = group?.name?.takeIf { it.isNotBlank() } ?: getString(R.string.group_chat)
            val composer = state.chatComposers[state.activeGroupId] ?: ChatComposer()
            if (binding.messageInput.text?.toString() != composer.text) {
                updatingDraft = true
                binding.messageInput.setText(composer.text)
                binding.messageInput.setSelection(composer.text.length)
                updatingDraft = false
            }
            binding.messageInput.isEnabled = !composer.sending
            binding.buttonSend.isEnabled = !composer.sending && composer.text.isNotBlank() && state.activeGroupId != null
            binding.buttonSend.contentDescription = getString(
                if (composer.failed) R.string.chat_retry_send else R.string.send_message
            )
            binding.messageLayout.error = if (composer.failed) getString(R.string.chat_send_failed) else null
            binding.textSendStatus.visibility = if (composer.sending) View.VISIBLE else View.GONE
            binding.chatLoadRetry.visibility = if (state.chatLoadFailed) View.VISIBLE else View.GONE
            if (renderedGroupId != state.activeGroupId || renderedMessages != state.messages) {
                renderMessages(binding, state.messages, renderedGroupId != state.activeGroupId || renderedMessages.isNullOrEmpty())
                renderedGroupId = state.activeGroupId
                renderedMessages = state.messages
            }
        }
    }

    private fun renderMessages(binding: FragmentChatBinding, messages: List<ChatMessage>, initial: Boolean) {
        val oldScrollY = binding.chatScroll.scrollY
        val nearBottom = !binding.chatScroll.canScrollVertically(1) ||
            binding.chatMessages.height - oldScrollY - binding.chatScroll.height < 48 * resources.displayMetrics.density
        binding.chatMessages.removeAllViews()
        messages.forEach { message ->
            val row = ItemChatMessageBinding.inflate(layoutInflater, binding.chatMessages, false)
            row.messageSender.text = message.senderName.ifBlank { getString(R.string.studyhub_member) }
            row.messageBody.text = message.text
            row.messageTime.text = DateUtils.formatTime(requireContext(), message.sentAt)
            binding.chatMessages.addView(row.root)
        }
        binding.chatScroll.post {
            if (view !== binding.root) return@post
            if (initial || nearBottom) binding.chatScroll.fullScroll(View.FOCUS_DOWN)
            else binding.chatScroll.scrollTo(0, oldScrollY)
        }
    }
}
