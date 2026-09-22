package com.studyhub.app.data.remote

import com.studyhub.app.data.model.QuoteDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The StudyHub REST API. The server lives in the /api folder of this repo and is
 * an Express service backed by Firestore - see the README for the deploy steps.
 */
interface StudyHubApi {

    @GET("api/quote/today")
    suspend fun getDailyQuote(): QuoteDto

    @GET("api/resources")
    suspend fun getResources(@Query("subject") subject: String? = null): List<ResourceDto>

    @POST("api/resources")
    suspend fun createResource(@Body body: ResourceDto): ResourceDto

    @PUT("api/resources/{id}")
    suspend fun updateResource(@Path("id") id: String, @Body body: ResourceDto): ResourceDto

    @DELETE("api/resources/{id}")
    suspend fun deleteResource(@Path("id") id: String)
}

data class ResourceDto(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val url: String = "",
    val addedBy: String = ""
)