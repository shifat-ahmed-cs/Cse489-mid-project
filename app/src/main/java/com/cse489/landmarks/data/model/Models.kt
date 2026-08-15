package com.cse489.landmarks.data.model

import com.google.gson.annotations.SerializedName

data class Landmark(
    val id: Int,
    val title: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val image: String?,
    @SerializedName("is_active") val isActive: Int = 1,
    @SerializedName("visit_count") val visitCount: Int = 0,
    @SerializedName("avg_distance") val avgDistance: Double = 0.0,
    val score: Double = 0.0
)

data class VisitRequest(
    @SerializedName("landmark_id") val landmarkId: Int,
    @SerializedName("user_lat") val userLat: Double,
    @SerializedName("user_lon") val userLon: Double
)

data class JobStatusResponse(
    @SerializedName("job_id") val jobId: Int?,
    val status: String?,
    val distance: Double?,
    val error: String?
)

data class CreateLandmarkResponse(
    val id: String?
)

data class ActionResponse(
    val status: String?,
    val error: String?
)

data class ErrorResponse(
    val error: String?
)
