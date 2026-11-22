package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.BillingType
import com.example.domain.model.CapabilityType
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DiscoveryStatus
import com.example.domain.model.EntitlementStatus
import com.example.domain.model.ExecutionEnvironment
import com.example.domain.model.ExperimentOutcome
import com.example.domain.model.HardwareTier
import com.example.domain.model.PrivacyTier
import com.example.domain.model.ServiceCategory
import com.example.domain.model.SourceType
import com.example.domain.model.UsageModel

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val vendor: String,
    val category: ServiceCategory,
    val summary: String,
    val officialDocsUrl: String,
    val pricingUrl: String,
    val isCloudHosted: Boolean,
    val supportsModelInferenceOnly: Boolean,
    val supportsRemoteBuild: Boolean,
    val supportsRemoteTest: Boolean,
    val supportsRemoteRender: Boolean,
    val supportsRemoteCompute: Boolean,
    val localHardwareRequirement: HardwareTier,
    val storageRequirementGb: Double,
    val privacyTier: PrivacyTier,
    val isSyntheticSample: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subscription_plans",
    foreignKeys = [
        ForeignKey(
            entity = ServiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["serviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("serviceId")]
)
data class SubscriptionPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceId: Long,
    val planName: String,
    val entitlementStatus: EntitlementStatus,
    val billingType: BillingType,
    /** Null when user has not entered or verified a fixed monthly cost. Never invented. */
    val monthlyFixedCostUsd: Double?,
    val meteredUnitCostDesc: String,
    val isUserVerifiedEntitlement: Boolean,
    val notes: String,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "capabilities",
    foreignKeys = [
        ForeignKey(
            entity = ServiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["serviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("serviceId")]
)
data class CapabilityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceId: Long,
    val capabilityName: String,
    val capabilityType: CapabilityType,
    val executionEnvironment: ExecutionEnvironment,
    val quotaSummary: String,
    val usageModel: UsageModel,
    /** Null when incremental cost is unknown/unverified. */
    val incrementalCostEstimateUsd: Double?,
    val costUnitLabel: String,
    val verificationSourceUrl: String,
    val verificationDateEpochMs: Long,
    val confidence: ConfidenceLevel,
    val isVerified: Boolean,
    val restrictionSummary: String,
    val alternativesNote: String,
    val notes: String
)

@Entity(
    tableName = "verification_sources",
    foreignKeys = [
        ForeignKey(
            entity = ServiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["serviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("serviceId")]
)
data class VerificationSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceId: Long,
    val title: String,
    val url: String,
    val sourceType: SourceType,
    val lastVerifiedDateEpochMs: Long,
    val confidence: ConfidenceLevel,
    val excerptNote: String,
    val lastHttpStatus: Int? = null,
    val lastFetchedTitle: String? = null,
    val lastCheckedEpochMs: Long? = null
)

@Entity(
    tableName = "capability_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = ServiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["serviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("serviceId")]
)
data class CapabilitySnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceId: Long,
    val snapshotLabel: String,
    val capturedAtEpochMs: Long,
    val planName: String,
    val entitlementStatus: EntitlementStatus,
    val monthlyCostUsd: Double?,
    /** JSON array of serialized capability entries for deterministic diffing. */
    val capabilitiesJson: String,
    val verificationSourceUrl: String,
    val notes: String
)

@Entity(
    tableName = "experiments",
    foreignKeys = [
        ForeignKey(
            entity = ServiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["serviceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("serviceId")]
)
data class ExperimentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceId: Long?,
    val title: String,
    val taskContext: String,
    val executionEnvironment: ExecutionEnvironment,
    val hypothesis: String,
    val observedResult: String,
    val outcome: ExperimentOutcome,
    val measuredCostUsd: Double?,
    val measuredLatencyMs: Long?,
    val recordedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "discoveries")
data class DiscoveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: ServiceCategory,
    val summary: String,
    val sourceUrl: String,
    val confidence: ConfidenceLevel,
    val status: DiscoveryStatus,
    val potentialAlternativeTo: String,
    val discoveredAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_combos")
data class FavoriteComboEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val taskProfile: String,
    val serviceIdsCsv: String,
    val rationale: String,
    val estimatedMonthlyCostUsd: Double?,
    val savedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_assessments")
data class SavedAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val taskQuery: String,
    val requirementsSummary: String,
    val topRecommendationName: String,
    val deterministicSummaryMarkdown: String,
    val optionalGeminiAnalysis: String?,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val isOfflinePinned: Boolean = true
)
