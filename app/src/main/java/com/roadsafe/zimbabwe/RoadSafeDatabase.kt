
package com.roadsafe.zimbabwe

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Incident::class],
    version = 3,
    exportSchema = false
)
abstract class RoadSafeDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao

    companion object {

        @Volatile
        private var INSTANCE: RoadSafeDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE incidents ADD COLUMN aiResult TEXT"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE incidents ADD COLUMN verificationStatus TEXT NOT NULL DEFAULT 'UNVERIFIED'"
                )
            }
        }

        fun getDatabase(context: Context): RoadSafeDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoadSafeDatabase::class.java,
                    "roadsafe_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
