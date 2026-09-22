package com.studyhub.app.data.repository

import android.util.Log
import com.studyhub.app.data.model.QuoteDto
import com.studyhub.app.data.remote.ResourceDto
import com.studyhub.app.data.remote.RetrofitClient
import com.studyhub.app.data.remote.StudyHubApi

/** Thin wrapper so the REST calls can be faked in unit tests. */
class ApiRepository(private val api: StudyHubApi = RetrofitClient.api) {

    companion object { private const val TAG = "ApiRepository" }

    suspend fun dailyQuote(): Result<QuoteDto> = runCatching { api.getDailyQuote() }
        .onFailure { Log.w(TAG, "Quote fetch failed, falling back to local copy", it) }
        .recoverCatching {
            QuoteDto(
                id = "fallback",
                content = "Small consistent effort beats one long night before the deadline.",
                author = "StudyHub"
            )
        }

    suspend fun resources(subject: String? = null): Result<List<ResourceDto>> =
        runCatching { api.getResources(subject) }

    suspend fun addResource(resource: ResourceDto): Result<ResourceDto> =
        runCatching { api.createResource(resource) }
}