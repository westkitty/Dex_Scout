package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DeltaKind
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.EntitlementStatus
import com.example.domain.model.UsageModel
import com.example.ui.theme.LocalScoutColors

@Composable
fun TelemetryBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UsageCostBadge(
    usageModel: UsageModel,
    costUsd: Double?,
    modifier: Modifier = Modifier
) {
    val scout = LocalScoutColors.current
    val (bg, fg, label) = when (usageModel) {
        UsageModel.FREE -> Triple(scout.freeCostContainer, scout.freeCost, "FREE · $0")
        UsageModel.INCLUDED -> Triple(scout.verifiedContainer, scout.verified, "INCLUDED · $0 inc.")
        UsageModel.METERED -> Triple(
            scout.staleWarningContainer,
            scout.staleWarning,
            costUsd?.let { "METERED · $${"%.2f".format(it)}" } ?: "METERED · Unverified"
        )
        UsageModel.PAID -> Triple(
            scout.alertContainer,
            scout.alert,
            costUsd?.let { "PAID · $${"%.2f".format(it)}" } ?: "PAID · Unverified"
        )
    }
    TelemetryBadge(text = label, containerColor = bg, contentColor = fg, modifier = modifier)
}

@Composable
fun EntitlementBadge(
    status: EntitlementStatus,
    modifier: Modifier = Modifier
) {
    val scout = LocalScoutColors.current
    val (bg, fg, icon) = when (status) {
        EntitlementStatus.VERIFIED_ACTIVE -> Triple(scout.verifiedContainer, scout.verified, Icons.Filled.Verified)
        EntitlementStatus.FREE_TIER -> Triple(scout.freeCostContainer, scout.freeCost, Icons.Filled.Verified)
        EntitlementStatus.TRIAL -> Triple(scout.staleWarningContainer, scout.staleWarning, Icons.Filled.Schedule)
        EntitlementStatus.INFERRED_UNVERIFIED -> Triple(scout.alertContainer, scout.alert, Icons.Filled.WarningAmber)
        EntitlementStatus.NOT_SUBSCRIBED -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
            null
        )
    }
    TelemetryBadge(
        text = status.label.uppercase(),
        containerColor = bg,
        contentColor = fg,
        icon = icon,
        modifier = modifier
    )
}

@Composable
fun ConfidenceBadge(
    confidence: ConfidenceLevel,
    isStale: Boolean,
    daysOld: Long,
    modifier: Modifier = Modifier
) {
    val scout = LocalScoutColors.current
    if (isStale) {
        TelemetryBadge(
            text = "STALE ${daysOld}d · ${confidence.label}",
            containerColor = scout.staleWarningContainer,
            contentColor = scout.staleWarning,
            icon = Icons.Filled.Schedule,
            modifier = modifier
        )
    } else {
        val (bg, fg) = when (confidence) {
            ConfidenceLevel.VERIFIED_HIGH -> scout.verifiedContainer to scout.verified
            ConfidenceLevel.PROBABLE_MEDIUM -> scout.staleWarningContainer to scout.staleWarning
            ConfidenceLevel.INFERRED_LOW, ConfidenceLevel.UNVERIFIED -> scout.alertContainer to scout.alert
        }
        TelemetryBadge(
            text = "${confidence.label} · ${daysOld}d",
            containerColor = bg,
            contentColor = fg,
            modifier = modifier
        )
    }
}

@Composable
fun ExecutionModeBadge(
    isInferenceOnly: Boolean,
    supportsRemoteCompute: Boolean,
    modifier: Modifier = Modifier
) {
    val scout = LocalScoutColors.current
    if (isInferenceOnly) {
        TelemetryBadge(
            text = "INFERENCE ONLY",
            containerColor = scout.staleWarningContainer,
            contentColor = scout.staleWarning,
            icon = Icons.Filled.ErrorOutline,
            modifier = modifier
        )
    } else if (supportsRemoteCompute) {
        TelemetryBadge(
            text = "REMOTE EXEC / BUILD",
            containerColor = scout.verifiedContainer,
            contentColor = scout.verified,
            icon = Icons.Filled.Terminal,
            modifier = modifier
        )
    } else {
        TelemetryBadge(
            text = "PLATFORM / DATA",
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Filled.Cloud,
            modifier = modifier
        )
    }
}

@Composable
fun DeltaKindBadge(kind: DeltaKind, modifier: Modifier = Modifier) {
    val scout = LocalScoutColors.current
    val (label, bg, fg) = when (kind) {
        DeltaKind.ADDED -> Triple("+ ADDED", scout.freeCostContainer, scout.freeCost)
        DeltaKind.CHANGED -> Triple("~ CHANGED", scout.staleWarningContainer, scout.staleWarning)
        DeltaKind.REMOVED -> Triple("- REMOVED", scout.alertContainer, scout.alert)
    }
    TelemetryBadge(text = label, containerColor = bg, contentColor = fg, modifier = modifier)
}

@Composable
fun OptionalAiBadge(modifier: Modifier = Modifier) {
    val scout = LocalScoutColors.current
    TelemetryBadge(
        text = "OPTIONAL GEMINI · AI LOGIC",
        containerColor = scout.aiAccentContainer,
        contentColor = scout.aiAccent,
        icon = Icons.Filled.AutoAwesome,
        modifier = modifier
    )
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun KpiTile(
    label: String,
    value: String,
    accentColor: Color,
    sublabel: String,
    modifier: Modifier = Modifier
) {
    val scout = LocalScoutColors.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, scout.hairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accentColor)
                )
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ScoreMeter(
    score: Int,
    isDisqualified: Boolean,
    modifier: Modifier = Modifier
) {
    val scout = LocalScoutColors.current
    val color = when {
        isDisqualified -> scout.alert
        score >= 75 -> scout.verified
        score >= 50 -> scout.staleWarning
        else -> scout.alert
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isDisqualified) "DISQUALIFIED" else "MATCH SCORE",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Text(
                text = "$score / 100",
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
