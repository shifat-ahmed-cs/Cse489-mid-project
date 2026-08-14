package com.cse489.landmarks.data.api

import com.cse489.landmarks.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface LandmarkApiService {

    companion object {
        const val BASE_URL = "https://labs.anontech.info/cse489/exm3/"
        const val STUDENT_KEY = "22301582"
    }

    @GET("api.php")
    suspend fun getLandmarks(
        @Query("action") action: String = "get_landmarks",
        @Query("key") key: String = STUDENT_KEY
    ): Response<List<Landmark>>

    @POST("api.php")
    suspend fun visitLandmark(
        @Body request: VisitRequest,
        @Query("action") action: String = "visit_landmark",
        @Query("key") key: String = STUDENT_KEY
    ): Response<JobStatusResponse>

    @GET("api.php")
    suspend fun getJobStatus(
        @Query("job_id") jobId: Int,
        @Query("action") action: String = "get_job_status",
        @Query("key") key: String = STUDENT_KEY
    ): Response<JobStatusResponse>

    @Multipart
    @POST("api.php")
    suspend fun createLandmark(
        @Part("title") title: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lon") lon: RequestBody,
        @Part image: MultipartBody.Part?,
        @Query("action") action: String = "create_landmark",
        @Query("key") key: String = STUDENT_KEY
    ): Response<CreateLandmarkResponse>

    @FormUrlEncoded
    @POST("api.php")
    suspend fun deleteLandmark(
        @Field("id") id: Int,
        @Query("action") action: String = "delete_landmark",
        @Query("key") key: String = STUDENT_KEY
    ): Response<ActionResponse>

    @FormUrlEncoded
    @POST("api.php")
    suspend fun restoreLandmark(
        @Field("id") id: Int,
        @Query("action") action: String = "restore_landmark",
        @Query("key") key: String = STUDENT_KEY
    ): Response<ActionResponse>
}
