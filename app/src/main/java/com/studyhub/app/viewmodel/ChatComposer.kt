package com.studyhub.app.viewmodel

data class ChatComposer(
    val text: String = "",
    val messageId: String? = null,
    val sending: Boolean = false,
    val failed: Boolean = false
) {
    fun edit(value: String): ChatComposer =
        if (sending || value == text) this else ChatComposer(text = value)

    fun begin(newId: String): ChatComposer =
        if (sending || text.isBlank()) this
        else copy(messageId = messageId ?: newId, sending = true, failed = false)

    fun finish(id: String, success: Boolean): ChatComposer = when {
        messageId != id || !sending -> this
        success -> ChatComposer()
        else -> copy(sending = false, failed = true)
    }
}
