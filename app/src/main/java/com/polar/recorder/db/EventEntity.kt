package com.polar.recorder.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val eventType: String, // PVC_RUN, PAC_RUN, AFIB, VT, VF, PAUSE
    val timestampMs: Long,
    val durationMs: Long,
    val detail: String,
    val hrAtEvent: Int
)
