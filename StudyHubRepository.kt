package com.studyhub.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studyhub.app.data.local.StudyHubDao
import com.studyhub.app.data.local.toEntity
import com.studyhub.app.data.local.toModel
import com.studyhub.app.data.model.ChatMessage
import com.studyhub.app.data.model.StudyGroup
import com.studyhub.app.data.model.StudySession
import com.studyhub.app.data.model.Task
import com.studyhub.app.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Everything that reads or writes Firestore. Uses Room as a local cache to
 * support offline access. Every read is keyed off [authUidFlow] so that
 * signing in AFTER this repository was constructed (the normal case - the
 * app builds its ViewModels before the user has logged in) correctly
 * re-subscribes every listener to the right user, instead of staying
 * permanently wired to an empty uid captured at construction time.
 */
class StudyHubRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val localDao: StudyHubDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "StudyHubRepository"
        private const val USERS = "users"
        private const val GROUPS = "groups"
        private const val TASKS = "tasks"
        private const val SESSIONS = "sessions"
        private const val MESSAGES = "messages"
    }

    /** Emits the current uid immediately, then again every time sign-in state changes. */
    private fun authUidFlow(): Flow<String> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { a -> trySend(a.currentUser?.uid.orEmpty()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // --- Profile ------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeProfile(): Flow<UserProfile?> = authUidFlow().flatMapLatest { currentUid ->
        localDao.observeProfile(currentUid)
            .map { it?.toModel() }
            .onStart {
                if (currentUid.isNotEmpty()) {
                    firestore.collection(USERS).document(currentUid)
                        .addSnapshotListener { snap, error ->
                            if (error == null && snap != null) {
                                snap.toObject(UserProfile::class.java)?.let { profile ->
                                    scope.launch { localDao.insertProfile(profile.toEntity()) }
                                }
                            }
                        }
                }
            }
    }

    suspend fun updateProfile(updates: Map<String, Any>): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid.orEmpty()
        firestore.collection(USERS).document(currentUid).update(updates).await()
    }

    // --- Groups -------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMyGroups(): Flow<List<StudyGroup>> = authUidFlow().flatMapLatest { currentUid ->
        localDao.observeAllGroups(currentUid)
            .map { list -> list.map { it.toModel() } }
            .onStart {
                if (currentUid.isNotEmpty()) {
                    firestore.collection(GROUPS)
                        .whereArrayContains("memberIds", currentUid)
                        .addSnapshotListener { snap, error ->
                            if (error == null && snap != null) {
                                val groups = snap.toObjects(StudyGroup::class.java)
                                scope.launch { localDao.insertGroups(groups.map { it.toEntity(currentUid) }) }
                            }
                        }
                }
            }
    }

    suspend fun createGroup(name: String, subject: String, description: String): Result<String> =
        runCatching {
            val currentUid = auth.currentUser?.uid.orEmpty()
            val group = StudyGroup(
                name = name, subject = subject, description = description,
                ownerId = currentUid, memberIds = listOf(currentUid)
            )
            val ref = firestore.collection(GROUPS).add(group).await()
            localDao.insertGroups(listOf(group.copy(id = ref.id).toEntity(currentUid)))
            ref.id
        }

    suspend fun joinGroup(groupId: String): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid.orEmpty()
        val ref = firestore.collection(GROUPS).document(groupId)
        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val members = (snap.get("memberIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (!members.contains(currentUid)) tx.update(ref, "memberIds", members + currentUid)
            null
        }.await()
    }

    suspend fun leaveGroup(groupId: String): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid.orEmpty()
        val ref = firestore.collection(GROUPS).document(groupId)
        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val members = (snap.get("memberIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            tx.update(ref, "memberIds", members - currentUid)
            null
        }.await()
    }

    // --- Tasks --------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTasks(): Flow<List<Task>> = authUidFlow().flatMapLatest { currentUid ->
        localDao.observeAllTasks(currentUid)
            .map { list -> list.map { it.toModel() } }
            .onStart {
                if (currentUid.isNotEmpty()) {
                    firestore.collection(TASKS)
                        .whereEqualTo("ownerId", currentUid)
                        .orderBy("dueDate", Query.Direction.ASCENDING)
                        .addSnapshotListener { snap, error ->
                            if (error == null && snap != null) {
                                val tasks = snap.toObjects(Task::class.java)
                                scope.launch { localDao.insertTasks(tasks.map { it.toEntity(currentUid) }) }
                            }
                        }
                }
            }
    }

    suspend fun addTask(task: Task): Result<String> = runCatching {
        val currentUid = auth.currentUser?.uid.orEmpty()
        val ref = firestore.collection(TASKS).add(task.copy(ownerId = currentUid)).await()
        localDao.insertTasks(listOf(task.copy(id = ref.id, ownerId = currentUid).toEntity(currentUid)))
        ref.id
    }

    suspend fun setTaskCompleted(taskId: String, completed: Boolean): Result<Unit> = runCatching {
        firestore.collection(TASKS).document(taskId).update("completed", completed).await()
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        firestore.collection(TASKS).document(taskId).delete().await()
        localDao.deleteTask(taskId)
    }

    // --- Study sessions -----------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSessions(): Flow<List<StudySession>> = authUidFlow().flatMapLatest { currentUid ->
        localDao.observeSessions(currentUid)
            .map { list -> list.map { it.toModel() } }
            .onStart {
                firestore.collection(SESSIONS)
                    .orderBy("startTime", Query.Direction.ASCENDING)
                    .addSnapshotListener { snap, error ->
                        if (error == null && snap != null) {
                            val sessions = snap.toObjects(StudySession::class.java)
                            scope.launch { localDao.insertSessions(sessions.map { it.toEntity(currentUid) }) }
                        }
                    }
            }
    }

    suspend fun addSession(session: StudySession): Result<String> = runCatching {
        val currentUid = auth.currentUser?.uid.orEmpty()
        val ref = firestore.collection(SESSIONS)
            .add(session.copy(ownerId = currentUid, attendeeIds = listOf(currentUid))).await()
        localDao.insertSessions(listOf(session.copy(id = ref.id, ownerId = currentUid, attendeeIds = listOf(currentUid)).toEntity(currentUid)))
        ref.id
    }

    suspend fun toggleAttendance(sessionId: String): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid.orEmpty()
        val ref = firestore.collection(SESSIONS).document(sessionId)
        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val going = (snap.get("attendeeIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            tx.update(ref, "attendeeIds", if (going.contains(currentUid)) going - currentUid else going + currentUid)
            null
        }.await()
    }

    // --- Group chat ---------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMessages(groupId: String): Flow<List<ChatMessage>> = authUidFlow().flatMapLatest { currentUid ->
        localDao.observeMessages(groupId, currentUid)
            .map { list -> list.map { it.toModel() } }
            .onStart {
                firestore.collection(GROUPS).document(groupId).collection(MESSAGES)
                    .orderBy("sentAt", Query.Direction.ASCENDING)
                    .addSnapshotListener { snap, error ->
                        if (error == null && snap != null) {
                            val messages = snap.toObjects(ChatMessage::class.java)
                            scope.launch { localDao.insertMessages(messages.map { it.toEntity(currentUid) }) }
                        }
                    }
            }
    }

    suspend fun sendMessage(groupId: String, senderName: String, text: String): Result<Unit> =
        runCatching {
            val currentUid = auth.currentUser?.uid.orEmpty()
            val message = ChatMessage(
                groupId = groupId, senderId = currentUid, senderName = senderName, text = text.trim()
            )
            firestore.collection(GROUPS).document(groupId)
                .collection(MESSAGES).add(message).await()
            Unit
        }
}