package com.cse489.landmarks.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import com.cse489.landmarks.data.api.ApiClient
import com.cse489.landmarks.data.api.LandmarkApiService
import com.cse489.landmarks.data.local.*
import com.cse489.landmarks.data.model.VisitRequest
import com.cse489.landmarks.worker.VisitSyncWorker
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class LandmarkRepository(
    private val dao: LandmarkDao,
    private val apiService: LandmarkApiService = ApiClient.apiService,
    private val context: Context
) {

    val activeLandmarks: LiveData<List<LandmarkEntity>> = dao.getActiveLandmarks()
    val deletedLandmarks: LiveData<List<LandmarkEntity>> = dao.getDeletedLandmarks()
    val visitHistory: LiveData<List<VisitHistoryEntity>> = dao.getVisitHistory()

    suspend fun refreshLandmarks(): Result<Unit> {
        return try {
            val response = apiService.getLandmarks()
            if (response.isSuccessful && response.body() != null) {
                val entities = response.body()!!
                    .filter { it.title != null && it.lat != null && it.lon != null }
                    .map { landmark ->
                    LandmarkEntity(
                        id = landmark.id,
                        title = landmark.title ?: "",
                        lat = landmark.lat ?: 0.0,
                        lon = landmark.lon ?: 0.0,
                        image = landmark.image,
                        isActive = landmark.isActive,
                        visitCount = landmark.visitCount,
                        avgDistance = landmark.avgDistance,
                        score = landmark.score
                    )
                }
                dao.insertLandmarks(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch landmarks: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun visitLandmark(
        landmarkId: Int,
        landmarkTitle: String,
        userLat: Double,
        userLon: Double,
        isOnline: Boolean
    ): Result<String> {
        return if (isOnline) {
            try {
                val request = VisitRequest(landmarkId, userLat, userLon)
                val response = apiService.visitLandmark(request)
                if (response.isSuccessful && response.body()?.jobId != null) {
                    val jobId = response.body()!!.jobId!!
                    dao.insertVisit(
                        VisitHistoryEntity(
                            jobId = jobId,
                            landmarkId = landmarkId,
                            landmarkTitle = landmarkTitle,
                            userLat = userLat,
                            userLon = userLon,
                            status = "pending"
                        )
                    )
                    enqueueVisitWorker()
                    Result.success("Visit requested. Job #$jobId created (pending).")
                } else {
                    val errorMsg = response.body()?.error ?: response.message()
                    dao.insertVisit(
                        VisitHistoryEntity(
                            landmarkId = landmarkId,
                            landmarkTitle = landmarkTitle,
                            userLat = userLat,
                            userLon = userLon,
                            status = "failed"
                        )
                    )
                    Result.failure(Exception("Visit request failed: $errorMsg"))
                }
            } catch (e: Exception) {
                // If network failed during attempt, queue offline
                queueOfflineVisit(landmarkId, landmarkTitle, userLat, userLon)
            }
        } else {
            queueOfflineVisit(landmarkId, landmarkTitle, userLat, userLon)
        }
    }

    private suspend fun queueOfflineVisit(
        landmarkId: Int,
        landmarkTitle: String,
        userLat: Double,
        userLon: Double
    ): Result<String> {
        dao.insertPendingVisit(
            PendingVisitEntity(
                landmarkId = landmarkId,
                landmarkTitle = landmarkTitle,
                userLat = userLat,
                userLon = userLon
            )
        )
        dao.insertVisit(
            VisitHistoryEntity(
                landmarkId = landmarkId,
                landmarkTitle = landmarkTitle,
                userLat = userLat,
                userLon = userLon,
                status = "queued_offline"
            )
        )
        enqueueVisitWorker()
        return Result.success("Offline: Visit queued locally. Will sync when online.")
    }

    suspend fun createLandmark(
        title: String,
        lat: Double,
        lon: Double,
        imageFile: File?
    ): Result<String> {
        return try {
            val titlePart = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val latPart = lat.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val lonPart = lon.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val imagePart = imageFile?.let { file ->
                val mimeType = when {
                    file.name.endsWith(".png", true) -> "image/png"
                    file.name.endsWith(".webp", true) -> "image/webp"
                    else -> "image/jpeg"
                }
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, requestFile)
            }

            val response = apiService.createLandmark(titlePart, latPart, lonPart, imagePart)
            if (response.isSuccessful && response.body()?.id != null) {
                refreshLandmarks()
                Result.success("Landmark created successfully (ID: ${response.body()?.id})")
            } else {
                Result.failure(Exception("Failed to create landmark: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLandmark(id: Int): Result<String> {
        return try {
            val response = apiService.deleteLandmark(id)
            if (response.isSuccessful && response.body()?.status == "deleted") {
                dao.updateSoftDelete(id, 0)
                Result.success("Landmark soft-deleted successfully.")
            } else {
                Result.failure(Exception("Failed to delete landmark: ${response.message()}"))
            }
        } catch (e: Exception) {
            // Local soft delete if network fails
            dao.updateSoftDelete(id, 0)
            Result.success("Soft-deleted locally. Sync will update server.")
        }
    }

    suspend fun restoreLandmark(id: Int): Result<String> {
        return try {
            val response = apiService.restoreLandmark(id)
            if (response.isSuccessful && response.body()?.status == "restored") {
                dao.updateSoftDelete(id, 1)
                Result.success("Landmark restored successfully.")
            } else {
                Result.failure(Exception("Failed to restore landmark: ${response.message()}"))
            }
        } catch (e: Exception) {
            dao.updateSoftDelete(id, 1)
            Result.success("Restored locally.")
        }
    }

    private fun enqueueVisitWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<VisitSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            VisitSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }
}
