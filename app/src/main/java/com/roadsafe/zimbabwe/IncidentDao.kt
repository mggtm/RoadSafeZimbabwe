package com.roadsafe.zimbabwe

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: Incident): Long

    @Query("SELECT * FROM incidents ORDER BY id DESC")
    fun getAllIncidents(): Flow<List<Incident>>

    // Retrieve both new and partially synchronised incidents.
    @Query("""
        SELECT * FROM incidents
        WHERE syncStatus IN ('PENDING', 'PARTIAL')
        ORDER BY id ASC
    """)
    suspend fun getIncidentsToSync(): List<Incident>

    @Query("""
        UPDATE incidents
        SET syncStatus = :status
        WHERE id = :incidentId
    """)
    suspend fun updateSyncStatus(
        incidentId: Int,
        status: String
    )

    @Query("DELETE FROM incidents WHERE id = :incidentId")
    suspend fun deleteIncident(incidentId: Int)

    @Query("""
        UPDATE incidents
        SET verificationStatus = 'VERIFIED'
        WHERE id = :incidentId
    """)
    suspend fun verifyIncident(incidentId: Int)
}