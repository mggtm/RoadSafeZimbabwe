
package com.roadsafe.zimbabwe

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class Incident(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String,
    val description: String,
    val latitude: Double?,
    val longitude: Double?,
    val photoPath: String?,
    val timestamp: String,
    val syncStatus: String = "PENDING",
    val aiResult: String? = null,
    val verificationStatus: String = "UNVERIFIED"
)
