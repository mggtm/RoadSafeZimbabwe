
package com.roadsafe.zimbabwe

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class IncidentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val tag = "RoadSafeSync"

        return try {

            Log.d(tag, "Starting synchronisation")

            val database =
                RoadSafeDatabase.getDatabase(applicationContext)

            val dao = database.incidentDao()

            RoadSafeSupabase.signInAnonymously()

            val client = RoadSafeSupabase.client

            val userId = client.auth.currentSessionOrNull()
                ?.user?.id
                ?: throw IllegalStateException(
                    "Supabase authentication failed"
                )

            Log.d(tag, "Authenticated user: $userId")

            val incidents = dao.getIncidentsToSync()

            Log.d(tag, "Incidents awaiting sync: ${incidents.size}")

            var photoFailures = false

            for (incident in incidents) {

                Log.d(tag, "Processing incident ${incident.id}")

                var cloudPhotoPath: String? = null

                val photoPath = incident.photoPath

                var photoUploaded = photoPath.isNullOrBlank()

                // Upload photograph.
                if (!photoPath.isNullOrBlank()) {

                    val photoFile = File(photoPath)

                    if (photoFile.exists()) {

                        try {

                            val uploadPath =
                                "$userId/incidents/incident_${incident.id}.jpg"

                            client.storage
                                .from("incident-photos")
                                .upload(
                                    path = uploadPath,
                                    data = photoFile.readBytes()
                                ) {
                                    upsert = true
                                }

                            cloudPhotoPath = uploadPath
                            photoUploaded = true

                            Log.d(
                                tag,
                                "PHOTO UPLOADED: $uploadPath"
                            )

                        } catch (e: Exception) {

                            photoFailures = true
                            photoUploaded = false

                            Log.e(
                                tag,
                                "PHOTO UPLOAD FAILED: ${incident.id}",
                                e
                            )
                        }

                    } else {

                        photoFailures = true
                        photoUploaded = false

                        Log.e(
                            tag,
                            "PHOTO FILE MISSING: $photoPath"
                        )
                    }
                }

                // Convert timestamp.
                val localFormat = SimpleDateFormat(
                    "dd MMM yyyy, HH:mm",
                    Locale.getDefault()
                ).apply {
                    isLenient = false
                }

                val parsedDate =
                    localFormat.parse(incident.timestamp)

                if (parsedDate == null) {
                    throw IllegalArgumentException(
                        "Invalid timestamp: ${incident.timestamp}"
                    )
                }

                val reportDate = parsedDate

                val isoFormat = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    Locale.US
                ).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }

                val cloudTimestamp =
                    isoFormat.format(reportDate)

                // Generate a stable cloud UUID.
                val cloudId = UUID.nameUUIDFromBytes(
                    "$userId:${incident.id}".toByteArray(Charsets.UTF_8)
                ).toString()

                // Prepare database record.
                val incidentData = buildJsonObject {

                    put("id", cloudId)

                    put("category", incident.category)

                    put("description", incident.description)

                    put(
                        "latitude",
                        incident.latitude?.let(::JsonPrimitive)
                            ?: JsonNull
                    )

                    put(
                        "longitude",
                        incident.longitude?.let(::JsonPrimitive)
                            ?: JsonNull
                    )

                    put(
                        "photo_path",
                        cloudPhotoPath?.let(::JsonPrimitive)
                            ?: JsonNull
                    )

                    put("occurred_at", cloudTimestamp)

                    put(
                        "ai_analysis",
                        incident.aiResult?.let(::JsonPrimitive)
                            ?: JsonNull
                    )

                    put("owner_id", userId)
                }

                // Upload as a JSON array for this SDK's overload.
                val incidentArray = JsonArray(
                    listOf(incidentData)
                )

                client.from("incidents").upsert(incidentArray)

                Log.d(
                    tag,
                    "DATABASE UPLOAD SUCCESSFUL: ${incident.id}"
                )

                // Update local status.
                val status =
                    if (photoUploaded) "SYNCED" else "PARTIAL"

                dao.updateSyncStatus(
                    incidentId = incident.id,
                    status = status
                )

                Log.d(
                    tag,
                    "INCIDENT ${incident.id} STATUS: $status"
                )
            }

            if (photoFailures) {

                Log.w(
                    tag,
                    "Photo failures detected. Retrying."
                )

                Result.retry()

            } else {

                Log.d(
                    tag,
                    "ALL INCIDENTS FULLY SYNCHRONISED"
                )

                Result.success()
            }

        } catch (e: Exception) {

            Log.e(
                tag,
                "SYNCHRONISATION FAILED",
                e
            )

            Result.retry()
        }
    }

    companion object {

        fun schedule(context: Context) {

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request =
                OneTimeWorkRequestBuilder<IncidentSyncWorker>()
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context)
                .enqueue(request)
        }
    }
}
