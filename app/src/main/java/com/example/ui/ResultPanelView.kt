package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Language
import com.example.model.SackScanResult
import com.example.model.Translations
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@Composable
fun ResultPanelView(
    result: SackScanResult,
    language: Language,
    modifier: Modifier = Modifier
) {
    var isDebugExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("result_panel_view"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFBFDFB),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6E8DF)),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title & status tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (result.isLowConfidence) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (result.isLowConfidence) Icons.Default.Warning else Icons.Default.Balance,
                            contentDescription = null,
                            tint = if (result.isLowConfidence) Color(0xFFE65100) else Color(0xFF0F6E56),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.estimatedWeightLabel(language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E2923)
                        )
                    )
                }

                // Confidence badge
                val confidenceColor = when {
                    result.confidence >= 70 -> Color(0xFF2E7D32)
                    result.confidence >= 40 -> Color(0xFFE65100)
                    else -> Color(0xFFC62828)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(confidenceColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${result.confidence}% Confidence",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = confidenceColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Hero: Large Weight Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.isLowConfidence) Color(0xFFFFF8E1) else Color(0xFFF1F8F4)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (result.isLowConfidence) Color(0xFFFFE082) else Color(0xFFC8E6C9)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (result.isLowConfidence) {
                        Text(
                            text = "0.0 kg",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFC62828),
                                fontSize = 38.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${Translations.weightRangeLabel(language)}: ${result.weightRangeKg}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF757575),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f", result.estimatedWeightKg),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F6E56),
                                    fontSize = 44.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "kg",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B4A12)
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Range pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0EFE6))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${Translations.weightRangeLabel(language)}: ${result.weightRangeKg}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F6E56)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Overall Confidence Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Translations.confidenceLabel(language),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    )
                    Text(
                        text = "${result.confidence}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                result.confidence >= 70 -> Color(0xFF2E7D32)
                                result.confidence >= 40 -> Color(0xFFE65100)
                                else -> Color(0xFFC62828)
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val animatedProgress by animateFloatAsState(
                    targetValue = (result.confidence / 100f).coerceIn(0f, 1f),
                    label = "confidence_progress"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        result.confidence >= 70 -> Color(0xFF2E7D32)
                        result.confidence >= 40 -> Color(0xFFF57C00)
                        else -> Color(0xFFD32F2F)
                    },
                    trackColor = Color(0xFFE0E0E0)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Grid: Dimensions & Volume & Fill Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = Translations.volumeLabel(language),
                    value = "${String.format(Locale.US, "%.1f", result.volumeLitres)} L",
                    icon = Icons.Default.WaterDrop,
                    iconTint = Color(0xFF0F6E56),
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = Translations.fillLevelLabel(language),
                    value = "${result.fillLevelPercent}%",
                    icon = Icons.Default.LinearScale,
                    iconTint = Color(0xFF6B4A12),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dimensions full row
            MetricCard(
                title = Translations.dimensionsLabel(language),
                value = result.dimensions.toDisplayString(),
                icon = Icons.Default.Straighten,
                iconTint = Color(0xFF0F6E56),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Condition Card
            if (result.condition.isNotBlank()) {
                SectionCard(
                    title = Translations.conditionLabel(language),
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF2E7D32)
                ) {
                    Text(
                        text = result.condition,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF2D3748),
                            lineHeight = 20.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Visual Evidence Card
            if (result.visualEvidence.isNotBlank()) {
                SectionCard(
                    title = Translations.visualEvidenceLabel(language),
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF0F6E56)
                ) {
                    Text(
                        text = result.visualEvidence,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF2D3748),
                            lineHeight = 20.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Summary
            if (result.summary.isNotBlank()) {
                SectionCard(
                    title = Translations.summaryLabel(language),
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF6B4A12)
                ) {
                    Text(
                        text = result.summary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF2D3748),
                            lineHeight = 20.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Technical Details List
            if (result.details.isNotEmpty()) {
                SectionCard(
                    title = Translations.technicalDetailsLabel(language),
                    icon = Icons.Default.Straighten,
                    iconColor = Color(0xFF0F6E56)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.details.forEach { detail ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF0F6E56),
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF374151),
                                        lineHeight = 18.sp,
                                        fontWeight = if (detail.contains("volume × assumed density") || detail.contains("கொள்ளளவு")) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // --- COLLAPSIBLE DEBUG PANEL (Requirement 2) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debug_panel_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDebugExpanded = !isDebugExpanded }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Debug Panel",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == Language.TAMIL) "பிழைத்திருத்த விவரங்கள் (Debug)" else "Debug Panel",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                            )
                            if (result.modelUsed.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE2E8F0))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = result.modelUsed,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF0F172A),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = if (isDebugExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isDebugExpanded) "Collapse" else "Expand",
                            tint = Color(0xFF475569)
                        )
                    }

                    AnimatedVisibility(
                        visible = isDebugExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Model Info & Fallbacks
                            DebugSection(
                                title = "1. Model Execution & Fallback History:",
                                content = buildString {
                                    append("Active Model: ").append(result.modelUsed.ifBlank { "Unknown" })
                                    if (result.modelAttempts.isNotEmpty()) {
                                        append("\nAttempts:\n")
                                        result.modelAttempts.forEach { attempt ->
                                            append(" • ").append(attempt).append("\n")
                                        }
                                    }
                                }
                            )

                            // 2. Gate Decision & Branch
                            DebugSection(
                                title = "2. Confidence Gate Branch Decision:",
                                content = result.branchTaken.ifBlank { "None recorded" }
                            )

                            // 3. Raw JSON from extract_evidence
                            DebugSection(
                                title = "3. Raw JSON (extract_evidence):",
                                content = result.rawEvidenceJson.ifBlank { "No raw JSON available" }
                            )

                            // 4. Final Assembled Result Object
                            val assembledJson = try {
                                val obj = JSONObject().apply {
                                    put("estimatedWeightKg", result.estimatedWeightKg)
                                    put("weightRangeKg", result.weightRangeKg)
                                    put("dimensions", JSONObject().apply {
                                        put("heightCm", result.dimensions.heightCm)
                                        put("widthCm", result.dimensions.widthCm)
                                        put("depthCm", result.dimensions.depthCm)
                                    })
                                    put("volumeLitres", result.volumeLitres)
                                    put("fillLevelPercent", result.fillLevelPercent)
                                    put("visualEvidence", result.visualEvidence)
                                    put("condition", result.condition)
                                    put("details", JSONArray(result.details))
                                    put("summary", result.summary)
                                    put("confidence", result.confidence)
                                    put("scaleReferenceUsed", result.scaleReferenceUsed)
                                    put("isLowConfidence", result.isLowConfidence)
                                    put("modelUsed", result.modelUsed)
                                }
                                obj.toString(2)
                            } catch (e: Exception) {
                                result.toString()
                            }

                            DebugSection(
                                title = "4. Final Assembled Result Object:",
                                content = assembledJson
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugSection(
    title: String,
    content: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F172A))
                .padding(8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    lineHeight = 16.sp
                )
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F7F5)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2EEE8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF55605A),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E2923)
                )
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBF9)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3EDE7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}
