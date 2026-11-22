package com.example.data.local

import androidx.room.TypeConverter
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

class ScoutConverters {
    @TypeConverter fun fromServiceCategory(v: ServiceCategory): String = v.name
    @TypeConverter fun toServiceCategory(v: String): ServiceCategory = ServiceCategory.valueOf(v)

    @TypeConverter fun fromCapabilityType(v: CapabilityType): String = v.name
    @TypeConverter fun toCapabilityType(v: String): CapabilityType = CapabilityType.valueOf(v)

    @TypeConverter fun fromExecutionEnvironment(v: ExecutionEnvironment): String = v.name
    @TypeConverter fun toExecutionEnvironment(v: String): ExecutionEnvironment = ExecutionEnvironment.valueOf(v)

    @TypeConverter fun fromUsageModel(v: UsageModel): String = v.name
    @TypeConverter fun toUsageModel(v: String): UsageModel = UsageModel.valueOf(v)

    @TypeConverter fun fromBillingType(v: BillingType): String = v.name
    @TypeConverter fun toBillingType(v: String): BillingType = BillingType.valueOf(v)

    @TypeConverter fun fromEntitlementStatus(v: EntitlementStatus): String = v.name
    @TypeConverter fun toEntitlementStatus(v: String): EntitlementStatus = EntitlementStatus.valueOf(v)

    @TypeConverter fun fromConfidenceLevel(v: ConfidenceLevel): String = v.name
    @TypeConverter fun toConfidenceLevel(v: String): ConfidenceLevel = ConfidenceLevel.valueOf(v)

    @TypeConverter fun fromHardwareTier(v: HardwareTier): String = v.name
    @TypeConverter fun toHardwareTier(v: String): HardwareTier = HardwareTier.valueOf(v)

    @TypeConverter fun fromPrivacyTier(v: PrivacyTier): String = v.name
    @TypeConverter fun toPrivacyTier(v: String): PrivacyTier = PrivacyTier.valueOf(v)

    @TypeConverter fun fromSourceType(v: SourceType): String = v.name
    @TypeConverter fun toSourceType(v: String): SourceType = SourceType.valueOf(v)

    @TypeConverter fun fromExperimentOutcome(v: ExperimentOutcome): String = v.name
    @TypeConverter fun toExperimentOutcome(v: String): ExperimentOutcome = ExperimentOutcome.valueOf(v)

    @TypeConverter fun fromDiscoveryStatus(v: DiscoveryStatus): String = v.name
    @TypeConverter fun toDiscoveryStatus(v: String): DiscoveryStatus = DiscoveryStatus.valueOf(v)
}
