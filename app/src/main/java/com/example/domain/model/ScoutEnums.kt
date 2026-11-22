package com.example.domain.model

enum class ServiceCategory(val label: String) {
    AI_MODEL("AI Model / LLM"),
    CLOUD_COMPUTE("Cloud Compute"),
    DEV_PLATFORM("Dev Platform"),
    BUILD_CI("Remote Build & CI"),
    RENDER_MEDIA("Render & Media"),
    STORAGE_DB("Storage & DB"),
    SUBSCRIPTION_BUNDLE("Suite Bundle")
}

enum class CapabilityType(
    val label: String,
    val shortLabel: String,
    val isRemoteComputeExecution: Boolean
) {
    MODEL_INFERENCE("Model Inference", "Inference", false),
    REMOTE_BUILD("Remote Build / Compile", "Remote Build", true),
    REMOTE_TEST("Remote Test Runner", "Remote Test", true),
    REMOTE_RENDER("Remote GPU Rendering", "Remote Render", true),
    CLOUD_COMPUTE("General Cloud Compute", "Cloud Compute", true),
    CODE_HOSTING("Repo & Artifact Hosting", "Code Host", false),
    VECTOR_STORAGE("Database / Vector Store", "Storage/DB", false),
    AGENT_SANDBOX("Sandboxed Code Execution", "Sandbox Exec", true)
}

enum class ExecutionEnvironment(val label: String, val isCloud: Boolean) {
    CLOUD_MANAGED("Cloud Managed", true),
    REMOTE_CONTAINER("Remote Container", true),
    LOCAL_DEVICE("Local Device", false),
    HYBRID("Hybrid Local + Cloud", true)
}

enum class UsageModel(val label: String) {
    FREE("Free"),
    INCLUDED("Included"),
    METERED("Metered"),
    PAID("Paid")
}

enum class BillingType(val label: String) {
    FREE("Free Tier"),
    INCLUDED("Included in Bundle"),
    METERED("Usage Metered"),
    PAID_FIXED("Fixed Subscription"),
    HYBRID("Base + Metered")
}

/**
 * Explicitly separates verified active entitlements from inferred or unverified access.
 * Inferred access MUST NEVER be treated as verified entitlement.
 */
enum class EntitlementStatus(val label: String, val isVerifiedAccess: Boolean) {
    VERIFIED_ACTIVE("Verified Active", true),
    FREE_TIER("Verified Free Tier", true),
    TRIAL("Active Trial", true),
    INFERRED_UNVERIFIED("Inferred (Unverified)", false),
    NOT_SUBSCRIBED("Not Subscribed", false)
}

enum class ConfidenceLevel(val label: String, val scoreWeight: Double) {
    VERIFIED_HIGH("Verified (High)", 1.0),
    PROBABLE_MEDIUM("Probable (Med)", 0.65),
    INFERRED_LOW("Inferred (Low)", 0.30),
    UNVERIFIED("Unverified", 0.05)
}

enum class HardwareTier(val label: String, val rank: Int) {
    NONE("No Local Compute (Phone/Thin)", 0),
    LOW("Basic Laptop (8GB RAM)", 1),
    MEDIUM("Dev Workstation (16-32GB)", 2),
    HIGH_GPU("Dedicated GPU Rig", 3)
}

enum class PrivacyTier(val label: String, val rank: Int) {
    PUBLIC_TRAINING("Standard / May Train", 0),
    STANDARD_CLOUD("Standard Cloud", 1),
    ENTERPRISE_SOC2("Enterprise / SOC2", 2),
    ZERO_DATA_RETENTION("Zero Data Retention", 3),
    LOCAL_ONLY("Local / Air-Gapped", 4)
}

enum class SourceType(val label: String) {
    OFFICIAL_DOCS("Official Docs"),
    PRICING_PAGE("Pricing & Quotas"),
    CHANGELOG("Release Changelog"),
    TERMS_PRIVACY("Privacy & Terms"),
    USER_BENCHMARK("Observed Benchmark")
}

enum class ExperimentOutcome(val label: String) {
    PASSED("Verified Pass"),
    PARTIAL("Partial / Caveats"),
    BLOCKED_RESTRICTION("Blocked by Restriction"),
    FAILED_QUOTA("Hit Quota Limit"),
    INCONCLUSIVE("Inconclusive")
}

enum class DiscoveryStatus(val label: String) {
    NEW("New Lead"),
    TRIAGED("Under Review"),
    VERIFIED_INTO_INVENTORY("Verified & Added"),
    DISMISSED("Dismissed")
}

enum class MatrixSortOption(val label: String) {
    NAME_ASC("Name A–Z"),
    COST_ASC("Cost Low → High"),
    CONFIDENCE_DESC("Highest Confidence"),
    FRESHNESS_DESC("Most Recently Verified"),
    STALEST_FIRST("Stalest Evidence First")
}
