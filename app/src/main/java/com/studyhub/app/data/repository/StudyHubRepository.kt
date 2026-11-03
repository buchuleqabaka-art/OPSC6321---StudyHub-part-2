package com.studyhub.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.studyhub.app.data.local.PendingTaskOperationEntity
import com.studyhub.app.data.local.StudyHubDao
import com.studyhub.app.data.local.TaskSyncCodec
import com.studyhub.app.data.local.toEntity
import com.studyhub.app.data.local.toModel
import com.studyhub.app.data.model.ChatMessage
import com.studyhub.app.data.model.StudyGroup
import com.studyhub.app.data.model.StudySession
import com.studyhub.app.data.model.Task
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.data.sync.TaskSyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.studyhub.app.util.PlanningPolicy

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
    private val localDao: StudyHubDao,
    private val appContext: Context,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "StudyHubRepository"
        private const val USERS = "users"
        private const val GROUPS = "groups"
        private const val TASKS = "tasks"
        private const val SESSIONS = "sessions"
        private const val MESSAGES = "messages"
        private const val MAX_PROFILE_PHOTO_BYTES = 5L * 1024L * 1024L
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
        require(currentUid.isNotBlank()) { "A signed-in account is required to update a profile" }
        firestore.collection(USERS).document(currentUid).update(updates).await()
    }

    suspend fun uploadProfilePhoto(uri: Uri): Result<String> = runCatching {
        val currentUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: error("A signed-in account is required to upload a profile photo")
        val mimeType = appContext.contentResolver.getType(uri)
            ?.takeIf { it.startsWith("image/") }
            ?: error("Choose a valid image file")
        val size = appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        require(size <= MAX_PROFILE_PHOTO_BYTES || size < 0) { "Profile photos must be smaller than 5 MB" }

        val imageRef = storage.reference.child("profile-photos/$currentUid/profile-image")
        val metadata = StorageMetadata.Builder().setContentType(mimeType).build()
        imageRef.putFile(uri, metadata).await()
        val downloadUrl = imageRef.downloadUrl.await().toString()
        firestore.collection(USERS).document(currentUid).update("photoUrl", downloadUrl).await()
        downloadUrl
    }

    // --- Groups -------------------------------------------------------------

    private fun snapshots(query: Query): Flow<QuerySnapshot> = callbackFlow {
        val listener = query.addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
            if (error != null) close(error) else if (value != null) trySend(value)
        }
        awaitClose { listener.remove() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMyGroups(): Flow<List<StudyGroup>> = authUidFlow().flatMapLatest { uid ->
        if (uid.isBlank()) return@flatMapLatest flowOf(emptyList())
        callbackFlow {
            val cache = launch { localDao.observeAllGroups(uid).collect { send(it.map { row -> row.toModel() }) } }
            val remote = launch {
                snapshots(firestore.collection(GROUPS).whereArrayContains("memberIds", uid)).collect { snap ->
                    val groups = snap.toObjects(StudyGroup::class.java).map { it.toEntity(uid) }
                    if (!snap.metadata.isFromCache) localDao.replaceGroups(uid, groups)
                    else localDao.insertGroups(groups)
                }
            }
            awaitClose { cache.cancel(); remote.cancel() }
        }
    }

    private suspend fun publishMember(groupId: String, uid: String) {
        val profile = firestore.collection(USERS).document(uid).get().await().toObject(UserProfile::class.java)
        firestore.collection(GROUPS).document(groupId).collection("members").document(uid)
            .set(mapOf("name" to (profile?.name ?: "").take(80))).await()
    }

    suspend fun createGroup(name: String, subject: String, description: String): Result<String> =
        runCatching {
            val currentUid = auth.currentUser?.uid.orEmpty()
            require(currentUid.isNotBlank() && name.trim().length in 1..100)
            val group = StudyGroup(
                name = name, subject = subject, description = description,
                ownerId = currentUid, memberIds = listOf(currentUid)
            )
            val ref = firestore.collection(GROUPS).add(group).await()
            localDao.insertGroups(listOf(group.copy(id = ref.id).toEntity(currentUid)))
            runCatching { publishMember(ref.id, currentUid) }
            ref.id
        }

    suspend fun joinGroup(groupId: String): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid.orEmpty()
        require(currentUid.isNotBlank() && groupId.matches(Regex("[A-Za-z0-9_-]{1,200}")))
        val ref = firestore.collection(GROUPS).document(groupId)
        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            require(snap.exists()) { "Group not found" }
            val members = (snap.get("memberIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (!members.contains(currentUid)) tx.update(ref, "memberIds", members + currentUid)
            null
        }.await()
        runCatching { publishMember(groupId, currentUid) }
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
    private fun planningSnapshots(collection: String, uid: String): Flow<List<QuerySnapshot>> =
        localDao.observeAllGroups(uid).map { rows -> rows.map { it.id }.sorted() }.distinctUntilChanged()
            .flatMapLatest { ids ->
                val queries = listOf(firestore.collection(collection).whereEqualTo("ownerId", uid)) +
                    ids.map { firestore.collection(collection).whereEqualTo("groupId", it) }
                combine(queries.map { snapshots(it) }) { it.toList() }
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTasks(): Flow<List<Task>> = authUidFlow().flatMapLatest { uid ->
        if (uid.isBlank()) return@flatMapLatest flowOf(emptyList())
        callbackFlow {
            TaskSyncScheduler.enqueue(appContext, uid)
            val cache = launch {
                combine(localDao.observeAllTasks(uid), localDao.observeAllGroups(uid)) { tasks, groups ->
                    val ids = groups.map { it.id }.toSet()
                    tasks.filter { it.ownerId == uid || it.groupId in ids }.map { it.toModel() }
                }.collect { send(it) }
            }
            val remote = launch {
                planningSnapshots(TASKS, uid).collect { snapshots ->
                    val tasks = snapshots.flatMap { it.toObjects(Task::class.java) }.distinctBy { it.id }
                    localDao.reconcileTasks(uid, tasks.map { it.toEntity(uid) }, snapshots.none { it.metadata.isFromCache })
                }
            }
            awaitClose { cache.cancel(); remote.cancel() }
        }
    }

    fun observePendingTaskCount(): Flow<Int> = authUidFlow().flatMapLatest { currentUid ->
        if (currentUid.isNotBlank()) TaskSyncScheduler.enqueue(appContext, currentUid)
        localDao.observePendingTaskOperationCount(currentUid)
    }

    fun enqueuePendingTaskSync(uid: String) = TaskSyncScheduler.enqueue(appContext, uid)

    suspend fun addTask(task: Task): Result<String> = runCatching {
        val currentUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: error("A signed-in account is required to create a task")
        val taskId = firestore.collection(TASKS).document().id
        require(PlanningPolicy.validTitle(task.title) && task.dueDate > System.currentTimeMillis())
        if (task.groupId.isNotBlank()) {
            val group = localDao.getGroups(currentUid).firstOrNull { it.id == task.groupId }
            require(group != null && currentUid in group.memberIds && task.assignedToId in group.memberIds)
        } else require(task.assignedToId.isBlank() || task.assignedToId == currentUid)
        val localTask = task.copy(id = taskId, ownerId = currentUid,
            assignedToId = task.assignedToId.ifBlank { currentUid })
        val operation = PendingTaskOperationEntity(
            operationId = UUID.randomUUID().toString(),
            localUserId = currentUid,
            taskId = taskId,
            operation = TaskSyncCodec.UPSERT,
            payload = TaskSyncCodec.encode(localTask)
        )
        localDao.saveTaskAndQueue(localTask.toEntity(currentUid), operation)
        TaskSyncScheduler.enqueue(appContext, currentUid)
        taskId
    }

    suspend fun setTaskCompleted(task: Task): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: error("A signed-in account is required to update a task")
        require(PlanningPolicy.canComplete(task, currentUid))
        val updated = task.copy(completed = !task.completed)
        val operation = PendingTaskOperationEntity(
            operationId = UUID.randomUUID().toString(),
            localUserId = currentUid,
            taskId = updated.id,
            operation = TaskSyncCodec.COMPLETE,
            payload = TaskSyncCodec.encode(updated)
        )
        localDao.saveTaskAndQueue(updated.toEntity(currentUid), operation)
        TaskSyncScheduler.enqueue(appContext, currentUid)
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: error("A signed-in account is required to delete a task")
        require(localDao.getTask(taskId, currentUid)?.ownerId == currentUid)
        val operation = PendingTaskOperationEntity(
            operationId = UUID.randomUUID().toString(),
            localUserId = currentUid,
            taskId = taskId,
            operation = TaskSyncCodec.DELETE,
            payload = ""
        )
        localDao.deleteTaskAndQueue(taskId, currentUid, operation)
        TaskSyncScheduler.enqueue(appContext, currentUid)
    }

    // --- Study sessions -----------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSessions(): Flow<List<StudySession>> = authUidFlow().flatMapLatest { uid ->
        if (uid.isBlank()) return@flatMapLatest flowOf(emptyList())
        callbackFlow {
            val cache = launch {
                combine(localDao.observeSessions(uid), localDao.observeAllGroups(uid)) { sessions, groups ->
                    val ids = groups.map { it.id }.toSet()
                    sessions.filter { it.ownerId == uid || it.groupId in ids }.map { it.toModel() }
                }.collect { send(it) }
            }
            val remote = launch {
                planningSnapshots(SESSIONS, uid).collect { snapshots ->
                    val sessions = snapshots.flatMap { it.toObjects(StudySession::class.java) }
                        .distinctBy { it.id }.map { it.toEntity(uid) }
                    if (snapshots.none { it.metadata.isFromCache }) localDao.replaceSessions(uid, sessions)
                    else localDao.insertSessions(sessions)
                }
            }
            awaitClose { cache.cancel(); remote.cancel() }
        }
    }

    suspend fun addSession(session: StudySession): Result<String> = runCatching {
        val currentUid = auth.currentUser?.uid.orEmpty()
        require(currentUid.isNotBlank())
        require(PlanningPolicy.validSession(session.title, session.startTime, session.endTime, System.currentTimeMillis()))
        if (session.groupId.isNotBlank()) require(localDao.getGroups(currentUid).any { it.id == session.groupId && currentUid in it.memberIds })
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
        if (currentUid.isBlank()) return@flatMapLatest flowOf(emptyList())
        callbackFlow {
            val cacheJob = launch {
                localDao.observeMessages(groupId, currentUid).collect { cached ->
                    send(cached.map { it.toModel() })
                }
            }
            val registration = firestore.collection(GROUPS).document(groupId).collection(MESSAGES)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        close(error)
                    } else if (snapshot != null) {
                        // Firestore emits local writes before the server accepts them. Keep those
                        // in the sending composer, not in the confirmed-message cache.
                        val confirmed = snapshot.documents.filterNot { it.metadata.hasPendingWrites() }
                            .mapNotNull { it.toObject(ChatMessage::class.java) }
                        launch { localDao.insertMessages(confirmed.map { it.toEntity(currentUid) }) }
                    }
                }
            awaitClose {
                registration.remove()
                cacheJob.cancel()
            }
        }
    }

    suspend fun sendMessage(groupId: String, senderName: String, text: String, messageId: String): Result<String> =
        runCatching {
            val currentUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
                ?: error("A signed-in account is required to send a message")
            val messageRef = firestore.collection(GROUPS).document(groupId)
                .collection(MESSAGES).document(messageId)
            val message = ChatMessage(
                id = messageRef.id,
                groupId = groupId,
                senderId = currentUid,
                senderName = senderName,
                text = text.trim()
            )
            require(message.text.isNotBlank() && message.text.length <= 2000)
            messageRef.set(message).await()
            messageRef.id
        }.onFailure { if (it is CancellationException) throw it }
}