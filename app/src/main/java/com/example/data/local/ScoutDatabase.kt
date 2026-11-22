package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ServiceEntity::class,
        SubscriptionPlanEntity::class,
        CapabilityEntity::class,
        VerificationSourceEntity::class,
        CapabilitySnapshotEntity::class,
        ExperimentEntity::class,
        DiscoveryEntity::class,
        FavoriteComboEntity::class,
        SavedAssessmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(ScoutConverters::class)
abstract class ScoutDatabase : RoomDatabase() {
    abstract fun scoutDao(): ScoutDao

    companion object {
        @Volatile
        private var INSTANCE: ScoutDatabase? = null

        fun getInstance(context: Context): ScoutDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScoutDatabase::class.java,
                    "dex_scout_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
