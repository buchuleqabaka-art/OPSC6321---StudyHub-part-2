package com.studyhub.app
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.studyhub.app.quiz.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
@RunWith(AndroidJUnit4::class)
class FeatureStorageTest {
    @Test fun appLaunchesWithoutAConfiguredApi() {
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> assertNotNull(activity.findViewById<android.view.View>(R.id.fragment_container)) }
        }
    }

    @Test fun savedQuizResumesOfflineAndIsIsolatedBetweenAccounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val a = QuizStore(context,"test-a-"+UUID.randomUUID()); val b = QuizStore(context,"test-b-"+UUID.randomUUID())
        val q = PaperQuiz("test","Paper",listOf(QuizQuestion("Question",listOf("A","B","C","D"),1,"Reason","Q1")),listOf(1))
        try { a.save(listOf(q)); assertEquals(q,a.load().single()); assertTrue(b.load().isEmpty()); assertEquals(1,a.load().single().score) }
        finally { a.save(emptyList()); b.save(emptyList()) }
    }
    @Test fun newFeatureLayoutsInflate() {
        val context = android.view.ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().targetContext,R.style.Theme_StudyHub)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val inflater=android.view.LayoutInflater.from(context)
            for(layout in listOf(R.layout.fragment_engagement,R.layout.fragment_quiz,R.layout.fragment_chat,R.layout.fragment_sessions)) assertNotNull(inflater.inflate(layout,null))
        }
    }
}
