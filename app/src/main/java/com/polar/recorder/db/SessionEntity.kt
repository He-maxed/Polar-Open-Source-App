package com.polar.recorder.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionName: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val meanHr: Int,
    val minHr: Int,
    val maxHr: Int,
    val totalBeats: Int,
    val pvcCount: Int,
    val pacCount: Int,
    val rmssdMs: Double,
    val sdnnMs: Double,
    val ecgFilePath: String,
    val rrFilePath: String,
    val accFilePath: String
)
