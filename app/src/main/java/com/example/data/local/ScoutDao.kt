package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoutDao {
    // --- Services ---
    @Query("SELECT * FROM services ORDER BY name ASC")
    fun observeAllServices(): Flow<List<ServiceEntity>>

    @Query("SELECT COUNT(*) FROM services")
    suspend fun getServiceCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceEntity): Long

    @Update
    suspend fun updateService(service: ServiceEntity)

    @Query("DELETE FROM services WHERE id = :serviceId")
    suspend fun deleteServiceById(serviceId: Long)

    // --- Subscription Plans ---
    @Query("SELECT * FROM subscription_plans ORDER BY serviceId ASC, id ASC")
    fun observeAllPlans(): Flow<List<SubscriptionPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SubscriptionPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: SubscriptionPlanEntity)

    @Query("DELETE FROM subscription_plans WHERE id = :planId")
    suspend fun deletePlanById(planId: Long)

    // --- Capabilities ---
    @Query("SELECT * FROM capabilities ORDER BY capabilityName ASC")
    fun observeAllCapabilities(): Flow<List<CapabilityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapability(capability: CapabilityEntity): Long

    @Update
    suspend fun updateCapability(capability: CapabilityEntity)

    @Query("DELETE FROM capabilities WHERE id = :capabilityId")
    suspend fun deleteCapabilityById(capabilityId: Long)

    // --- Verification Sources ---
    @Query("SELECT * FROM verification_sources ORDER BY lastVerifiedDateEpochMs DESC")
    fun observeAllSources(): Flow<List<VerificationSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: VerificationSourceEntity): Long

    @Update
    suspend fun updateSource(source: VerificationSourceEntity)

    @Query("DELETE FROM verification_sources WHERE id = :sourceId")
    suspend fun deleteSourceById(sourceId: Long)

    // --- Capability Snapshots ---
    @Query("SELECT * FROM capability_snapshots ORDER BY capturedAtEpochMs DESC")
    fun observeAllSnapshots(): Flow<List<CapabilitySnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: CapabilitySnapshotEntity): Long

    @Query("DELETE FROM capability_snapshots WHERE id = :snapshotId")
    suspend fun deleteSnapshotById(snapshotId: Long)

    // --- Experiments ---
    @Query("SELECT * FROM experiments ORDER BY recordedAtEpochMs DESC")
    fun observeAllExperiments(): Flow<List<ExperimentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperiment(experiment: ExperimentEntity): Long

    @Query("DELETE FROM experiments WHERE id = :experimentId")
    suspend fun deleteExperimentById(experimentId: Long)

    // --- Discoveries ---
    @Query("SELECT * FROM discoveries ORDER BY discoveredAtEpochMs DESC")
    fun observeAllDiscoveries(): Flow<List<DiscoveryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscovery(discovery: DiscoveryEntity): Long

    @Update
    suspend fun updateDiscovery(discovery: DiscoveryEntity)

    @Query("DELETE FROM discoveries WHERE id = :discoveryId")
    suspend fun deleteDiscoveryById(discoveryId: Long)

    // --- Favorite Combos ---
    @Query("SELECT * FROM favorite_combos ORDER BY savedAtEpochMs DESC")
    fun observeAllCombos(): Flow<List<FavoriteComboEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombo(combo: FavoriteComboEntity): Long

    @Query("DELETE FROM favorite_combos WHERE id = :comboId")
    suspend fun deleteComboById(comboId: Long)

    // --- Saved Assessments (Offline Reading) ---
    @Query("SELECT * FROM saved_assessments ORDER BY createdAtEpochMs DESC")
    fun observeAllAssessments(): Flow<List<SavedAssessmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: SavedAssessmentEntity): Long

    @Query("DELETE FROM saved_assessments WHERE id = :assessmentId")
    suspend fun deleteAssessmentById(assessmentId: Long)

    // --- Full-text SQL search helpers ---
    @Query(
        """
        SELECT * FROM services
        WHERE name LIKE '%' || :query || '%'
           OR vendor LIKE '%' || :query || '%'
           OR summary LIKE '%' || :query || '%'
        ORDER BY name ASC
        """
    )
    suspend fun searchServices(query: String): List<ServiceEntity>

    @Query(
        """
        SELECT * FROM capabilities
        WHERE capabilityName LIKE '%' || :query || '%'
           OR quotaSummary LIKE '%' || :query || '%'
           OR restrictionSummary LIKE '%' || :query || '%'
           OR alternativesNote LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY capabilityName ASC
        """
    )
    suspend fun searchCapabilities(query: String): List<CapabilityEntity>

    @Query("DELETE FROM services")
    suspend fun clearAllServices()

    @Query("DELETE FROM discoveries")
    suspend fun clearAllDiscoveries()

    @Query("DELETE FROM favorite_combos")
    suspend fun clearAllCombos()

    @Query("DELETE FROM saved_assessments")
    suspend fun clearAllAssessments()

    @Query("DELETE FROM experiments")
    suspend fun clearAllExperiments()
}
