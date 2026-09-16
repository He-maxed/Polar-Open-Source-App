package com.polar.recorder.db

import androidx.room.*

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTimeMs DESC")
    fun getAllSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: SessionEntity): Long

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    fun deleteSession(sessionId: Long)

    @Query("SELECT * FROM events WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun getEventsForSession(sessionId: Long): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEvents(events: List<EventEntity>)
}
