package com.cse489.landmarks.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "landmarks")
data class LandmarkEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val lat: Double,
    val lon: Double,
    val image: String?,
    val isActive: Int,
    val visitCount: Int,
    val avgDistance: Double,
    val score: Double
)

@Entity(tableName = "visit_history")
data class VisitHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Int? = null,
    val landmarkId: Int,
    val landmarkTitle: String,
    val visitTime: Long = System.currentTimeMillis(),
    val userLat: Double,
    val userLon: Double,
    val distance: Double? = null,
    val status: String
)

@Entity(tableName = "pending_visits")
data class PendingVisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val landmarkId: Int,
    val landmarkTitle: String,
    val userLat: Double,
    val userLon: Double,
    val visitTime: Long = System.currentTimeMillis()
)
