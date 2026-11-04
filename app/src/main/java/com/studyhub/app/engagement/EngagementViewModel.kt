package com.studyhub.app.engagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

data class EngagementState(val uid: String? = null, val records: Map<String, String> = emptyMap(),
    val polls: List<GroupPoll> = emptyList(), val busy: Boolean = false, val failed: Boolean = false,
    val attendanceLoaded: Boolean = false, val pollsLoaded: Boolean = false, val cache: Boolean = true)
@OptIn(ExperimentalCoroutinesApi::class)
class EngagementViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val mutable = MutableStateFlow(EngagementState())
    val state = mutable.asStateFlow()
    private var group: String? = null
    private var attendanceJob: Job? = null
    private var pollJob: Job? = null
    private var writeJob: Job? = null
    private val listener = FirebaseAuth.AuthStateListener { reset(it.currentUser?.uid) }
    init { auth.addAuthStateListener(listener) }
    private fun snapshots(query: Query): Flow<QuerySnapshot> = callbackFlow {
        val registration = query.addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
            if (error != null) close(error) else if (value != null) trySend(value)
        }
        awaitClose { registration.remove() }
    }
    private fun reset(uid: String?) {
        if (mutable.value.uid == uid && attendanceJob != null) return
        attendanceJob?.cancel(); pollJob?.cancel(); writeJob?.cancel()
        mutable.value = EngagementState(uid = uid)
        if (uid == null) return
        attendanceJob = viewModelScope.launch {
            snapshots(db.collection("users").document(uid).collection("attendance"))
                .catch { mutable.update { it.copy(failed = true, records = emptyMap(), attendanceLoaded = false) } }
                .collect { snapshot -> mutable.update { it.copy(records = snapshot.documents.associate { d -> d.id to d.getString("status").orEmpty() },
                    attendanceLoaded = true, cache = snapshot.metadata.isFromCache) } }
        }
        observePolls()
    }
    fun bindGroup(id: String?) { if (group != id) { group = id; observePolls() } }
    fun refresh() { attendanceJob?.cancel(); attendanceJob = null; reset(auth.currentUser?.uid) }
    private fun observePolls() {
        pollJob?.cancel(); mutable.update { it.copy(polls = emptyList(), pollsLoaded = false) }
        val uid = mutable.value.uid ?: return
        val id = group ?: return
        pollJob = viewModelScope.launch {
            snapshots(db.collection("groups").document(id).collection("polls").orderBy("createdAt", Query.Direction.DESCENDING).limit(30))
                .flatMapLatest { snapshot ->
                    val polls = snapshot.documents.map { d -> GroupPoll(d.id, d.getString("question").orEmpty(),
                        (d.get("options") as? List<*>)?.filterIsInstance<String>().orEmpty(), d.getString("ownerId").orEmpty(), d.getBoolean("closed") ?: true) }
                    if (polls.isEmpty()) flowOf(emptyList()) else combine(polls.map { poll ->
                        snapshots(db.collection("groups").document(id).collection("polls").document(poll.id).collection("votes"))
                            .map { votes -> poll.copy(votes = votes.documents.mapNotNull { v ->
                                val choice = v.getLong("choice")?.toInt()
                                if (choice != null && choice in poll.options.indices) v.id to choice else null
                            }.toMap()) }
                    }) { it.toList() }
                }.catch { if (mutable.value.uid == uid) mutable.update { it.copy(failed = true, polls = emptyList(), pollsLoaded = false) } }
                .collect { polls -> if (mutable.value.uid == uid) mutable.update { it.copy(polls = polls, pollsLoaded = true) } }
        }
    }
    private fun write(action: suspend (String) -> Unit) {
        if (mutable.value.busy) return
        val uid = auth.currentUser?.uid ?: return
        mutable.update { it.copy(busy = true, failed = false) }
        writeJob = viewModelScope.launch {
            try { action(uid) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (mutable.value.uid == uid) mutable.update { it.copy(failed = true) } }
            finally { if (mutable.value.uid == uid) mutable.update { it.copy(busy = false) } }
        }
    }
    fun mark(sessionId: String, status: String?) = write { uid ->
        require(status == null || status in listOf("attended", "absent"))
        val ref = db.collection("users").document(uid).collection("attendance").document(sessionId)
        db.runTransaction { tx ->
            val session = tx.get(db.collection("sessions").document(sessionId))
            check(session.exists() && (session.getLong("endTime") ?: Long.MAX_VALUE) <= System.currentTimeMillis())
            if (status == null) tx.delete(ref) else tx.set(ref, mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp()))
        }.await()
    }
    fun create(question: String, options: List<String>) = write { uid ->
        val id = group ?: error("Group required")
        require(PollPolicy.valid(question, options))
        val ref = db.collection("groups").document(id).collection("polls").document()
        db.runTransaction { tx ->
            val members = tx.get(db.collection("groups").document(id)).get("memberIds") as? List<*>
            check(members?.contains(uid) == true)
            tx.set(ref, mapOf("question" to question.trim(), "options" to options.map { it.trim() }, "ownerId" to uid,
                "closed" to false, "createdAt" to FieldValue.serverTimestamp()))
        }.await()
    }
    fun vote(pollId: String, choice: Int) = write { uid ->
        val id = group ?: error("Group required")
        val poll = db.collection("groups").document(id).collection("polls").document(pollId)
        db.runTransaction { tx ->
            val snapshot = tx.get(poll)
            val options = snapshot.get("options") as? List<*> ?: emptyList<Any>()
            check(snapshot.exists() && snapshot.getBoolean("closed") == false && choice in options.indices)
            tx.set(poll.collection("votes").document(uid), mapOf("choice" to choice))
        }.await()
    }
    fun closePoll(pollId: String) = write { uid ->
        val id = group ?: error("Group required")
        val ref = db.collection("groups").document(id).collection("polls").document(pollId)
        db.runTransaction { tx ->
            check(tx.get(ref).getString("ownerId") == uid); tx.update(ref, "closed", true)
        }.await()
    }
    override fun onCleared() { auth.removeAuthStateListener(listener); super.onCleared() }
}
