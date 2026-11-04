package com.studyhub.app.util
import com.studyhub.app.engagement.*
import com.studyhub.app.data.model.StudySession
import org.junit.Assert.*
import org.junit.Test
class EngagementPolicyTest {
    @Test fun ignoresFutureUnrecordedAndDuplicateSessionsInRatio() {
        val sessions = listOf(StudySession(id="a",endTime=5),StudySession(id="a",endTime=5),StudySession(id="b",endTime=5),StudySession(id="c",endTime=5),StudySession(id="future",endTime=100))
        val result = AttendancePolicy.summarize(sessions,mapOf("a" to "attended", "b" to "absent", "future" to "absent"),10)
        assertEquals(AttendanceSummary(1,1,1),result); assertEquals(50,result.percent)
        assertNull(AttendancePolicy.summarize(sessions,emptyMap(),10).percent)
    }
    @Test fun correctionReplacesStatusAndMissingIsNotAbsent() {
        val sessions = listOf(StudySession(id="a",endTime=5))
        assertEquals(100,AttendancePolicy.summarize(sessions,mapOf("a" to "attended"),10).percent)
        assertEquals(0,AttendancePolicy.summarize(sessions,mapOf("a" to "absent"),10).percent)
        assertEquals(1,AttendancePolicy.summarize(sessions,emptyMap(),10).unrecorded)
    }
    @Test fun validatesPollsAndCountsOneCurrentVotePerMember() {
        assertTrue(PollPolicy.valid("When?",listOf("Today","Tomorrow")))
        assertFalse(PollPolicy.valid(" ",listOf("A","B")))
        assertFalse(PollPolicy.valid("When?",listOf(" A ","a")))
        assertFalse(PollPolicy.valid("When?",listOf("One")))
        assertFalse(PollPolicy.valid("When?",listOf("x".repeat(101),"B")))
        val poll = GroupPoll("p","When?",listOf("A","B"),"alice",false,mapOf("alice" to 0,"bob" to 1))
        assertEquals(listOf(1,1),poll.counts)
        assertEquals(listOf(0,2),poll.copy(votes=poll.votes + ("alice" to 1)).counts)
    }
}
