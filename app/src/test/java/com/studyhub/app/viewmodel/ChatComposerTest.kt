package com.studyhub.app.viewmodel

import org.junit.Assert.*
import org.junit.Test

class ChatComposerTest {
    @Test fun `failed send preserves text and retry reuses message id`() {
        val failed = ChatComposer(text = "Study at 3?").begin("first").finish("first", false)
        assertEquals("Study at 3?", failed.text)
        assertTrue(failed.failed)
        assertFalse(failed.sending)
        val retry = failed.begin("second")
        assertEquals("first", retry.messageId)
        assertTrue(retry.sending)
        assertFalse(retry.failed)
    }

    @Test fun `successful acknowledgement clears the draft`() {
        assertEquals(ChatComposer(), ChatComposer(text = "Hello").begin("id").finish("id", true))
    }

    @Test fun `sending cannot be edited or submitted twice`() {
        val sending = ChatComposer(text = "Hello").begin("id")
        assertEquals(sending, sending.edit("Changed"))
        assertEquals(sending, sending.begin("duplicate"))
    }

    @Test fun `editing after a failure starts a new message`() {
        val edited = ChatComposer(text = "Hello").begin("old").finish("old", false).edit("New text")
        assertEquals("new", edited.begin("new").messageId)
        assertFalse(edited.failed)
    }

    @Test fun `late acknowledgement cannot clear a different attempt`() {
        val sending = ChatComposer(text = "New text").begin("new")
        assertEquals(sending, sending.finish("old", true))
    }

    @Test fun `blank draft is not submitted`() {
        val blank = ChatComposer(text = "   ")
        assertEquals(blank, blank.begin("id"))
    }
}
