package com.studyhub.app.data.remote

import com.studyhub.app.data.model.QuoteDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The StudyHub REST API, served by the Express/Firebase Admin service in the
 * repository root. See the README for deployment and authentication details.
 */
interface StudyHubApi {

    @GET("api/quote/today")
    suspend fun getDailyQuote(@Query("lang") languageTag: String? = null): QuoteDto

    @GET("api/resources")
    suspend fun getResources(@Query("subject") subject: String? = null): List<ResourceDto>

    @POST("api/resources")
    suspend fun createResource(
        @Header("Authorization") bearerToken: String,
        @Body body: ResourceDto
    ): ResourceDto

    @PUT("api/resources/{id}")
    suspend fun updateResource(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Body body: ResourceDto
    ): ResourceDto

    @DELETE("api/resources/{id}")
    suspend fun deleteResource(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String
    ): Response<Unit>

    @POST("api/notifications/register")
    suspend fun registerPushToken(
        @Header("Authorization") bearerToken: String,
        @Body body: PushTokenRequest
    ): Response<Unit>

    @POST("api/notifications/unregister")
    suspend fun unregisterPushToken(
        @Header("Authorization") bearerToken: String,
        @Body body: PushTokenRequest
    ): Response<Unit>

    @POST("api/notifications/group-message")
    suspend fun notifyGroupMessage(
        @Header("Authorization") bearerToken: String,
        @Body body: GroupMessageNotificationRequest
    ): Response<Unit>
}

data class ResourceDto(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val url: String = "",
    val addedBy: String = ""
)

data class PushTokenRequest(
    val token: String,
    val platform: String = "android"
)

data class GroupMessageNotificationRequest(
    val groupId: String,
    val messageId: String
)