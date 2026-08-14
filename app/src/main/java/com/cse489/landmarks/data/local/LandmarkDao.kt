package com.cse489.landmarks.data.local

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LandmarkDao {

    // Landmarks
    @Query("SELECT * FROM landmarks WHERE isActive = 1 ORDER BY score DESC")
    fun getActiveLandmarks(): LiveData<List<LandmarkEntity>>

    @Query("SELECT * FROM landmarks WHERE isActive = 0")
    fun getDeletedLandmarks(): LiveData<List<LandmarkEntity>>

    @Query("SELECT * FROM landmarks WHERE isActive = 1 ORDER BY score DESC")
    suspend fun getActiveLandmarksSync(): List<LandmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLandmarks(landmarks: List<LandmarkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLandmark(landmark: LandmarkEntity)

    @Query("UPDATE landmarks SET isActive = :isActive WHERE id = :id")
    suspend fun updateSoftDelete(id: Int, isActive: Int)

    @Query("DELETE FROM landmarks WHERE id = :id")
    suspend fun deleteLandmarkById(id: Int)

    // Visit History
    @Query("SELECT * FROM visit_history ORDER BY visitTime DESC")
    fun getVisitHistory(): LiveData<List<VisitHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitHistoryEntity): Long

    @Query("UPDATE visit_history SET status = :status, distance = :distance WHERE jobId = :jobId")
    suspend fun updateVisitStatusAndDistance(jobId: Int, status: String, distance: Double?)

    @Query("UPDATE visit_history SET jobId = :jobId, status = :status, distance = :distance WHERE id = :historyId")
    suspend fun updateVisitStatusByHistoryId(historyId: Long, jobId: Int?, status: String, distance: Double?)

    // Pending Visits for Offline Queue
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingVisit(pending: PendingVisitEntity): Long

    @Query("SELECT * FROM pending_visits ORDER BY visitTime ASC")
    suspend fun getPendingVisits(): List<PendingVisitEntity>

    @Query("DELETE FROM pending_visits WHERE id = :id")
    suspend fun deletePendingVisit(id: Long)
}
