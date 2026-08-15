package com.cse489.landmarks.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cse489.landmarks.data.api.ApiClient
import com.cse489.landmarks.data.local.AppDatabase
import com.cse489.landmarks.data.local.VisitHistoryEntity
import com.cse489.landmarks.data.model.VisitRequest
import kotlinx.coroutines.delay

class VisitSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "VisitSyncWorkerJob"
    }

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.landmarkDao()
        val apiService = ApiClient.apiService

        try {
            // Step 1: Drain offline queued visit requests
            val pendingVisits = dao.getPendingVisits()
            for (pending in pendingVisits) {
                try {
                    val request = VisitRequest(pending.landmarkId, pending.userLat, pending.userLon)
                    val response = apiService.visitLandmark(request)
                    if (response.isSuccessful && response.body()?.jobId != null) {
                        val jobId = response.body()!!.jobId!!
                        dao.insertVisit(
                            VisitHistoryEntity(
                                jobId = jobId,
                                landmarkId = pending.landmarkId,
                                landmarkTitle = pending.landmarkTitle,
                                visitTime = pending.visitTime,
                                userLat = pending.userLat,
                                userLon = pending.userLon,
                                status = "pending"
                            )
                        )
                        dao.deletePendingVisit(pending.id)
                    }
                } catch (e: Exception) {
                    // Retry later if network drops
                }
            }

            // Step 2: Poll status for any pending jobs in database
            var pollAttempts = 0
            val maxAttempts = 5

            while (pollAttempts < maxAttempts) {
                val pendingJobs = dao.getPendingJobVisits()

                if (pendingJobs.isEmpty()) {
                    break
                }

                for (visit in pendingJobs) {
                    val jobId = visit.jobId ?: continue
                    try {
                        val statusResponse = apiService.getJobStatus(jobId)
                        if (statusResponse.isSuccessful && statusResponse.body() != null) {
                            val body = statusResponse.body()!!
                            when (body.status) {
                                "done" -> {
                                    val distance = body.distance ?: 0.0
                                    dao.updateVisitStatusAndDistance(jobId, "done", distance)
                                }
                                "failed" -> {
                                    dao.updateVisitStatusAndDistance(jobId, "failed", null)
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Connection issue during polling
                    }
                }

                pollAttempts++
                delay(3000) // Wait 3 seconds before next poll attempt
            }

            // Refresh landmarks to update scores & visit counts
            try {
                val landmarksResponse = apiService.getLandmarks()
                if (landmarksResponse.isSuccessful && landmarksResponse.body() != null) {
                    val entities = landmarksResponse.body()!!
                        .filter { it.title != null && it.lat != null && it.lon != null }
                        .map { landmark ->
                        com.cse489.landmarks.data.local.LandmarkEntity(
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
                }
            } catch (_: Exception) {}

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
