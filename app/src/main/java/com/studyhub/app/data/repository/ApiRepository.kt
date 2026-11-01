package com.studyhub.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.studyhub.app.data.model.QuoteDto
import com.studyhub.app.data.remote.GroupMessageNotificationRequest
import com.studyhub.app.data.remote.ResourceDto
import com.studyhub.app.data.remote.RetrofitClient
import com.studyhub.app.data.remote.StudyHubApi
import kotlinx.coroutines.tasks.await

/** Thin wrapper so the REST calls can be faked in unit tests. */
class ApiRepository(
    private val api: StudyHubApi = RetrofitClient.api,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object { private const val TAG = "ApiRepository" }

    suspend fun dailyQuote(languageTag: String = "en"): Result<QuoteDto> =
        runCatching { api.getDailyQuote(languageTag) }
            .onFailure { Log.w(TAG, "Quote fetch failed; the localized in-app quote will be shown", it) }

    suspend fun resources(subject: String? = null): Result<List<ResourceDto>> =
        runCatching { api.getResources(subject) }

    suspend fun addResource(resource: ResourceDto): Result<ResourceDto> = runCatching {
        api.createResource(authorizationHeader(), resource)
    }

    suspend fun updateResource(id: String, resource: ResourceDto): Result<ResourceDto> = runCatching {
        api.updateResource(authorizationHeader(), id, resource)
    }

    suspend fun deleteResource(id: String): Result<Unit> = runCatching {
        val response = api.deleteResource(authorizationHeader(), id)
        check(response.isSuccessful) { "Resource endpoint returned ${response.code()}" }
    }

    suspend fun notifyGroupMessage(
        idToken: String,
        groupId: String,
        messageId: String
    ): Result<Unit> = runCatching {
        val response = api.notifyGroupMessage(
            "Bearer $idToken",
            GroupMessageNotificationRequest(groupId, messageId)
        )
        check(response.isSuccessful) { "Push endpoint returned ${response.code()}" }
    }

    private suspend fun authorizationHeader(): String {
        val user = auth.currentUser ?: error("Sign in to manage study resources")
        val token = user.getIdToken(false).await().token
            ?: error("Could not get a Firebase sign-in token")
        return "Bearer $token"
    }
}
