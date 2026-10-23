package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AgentNode
import com.example.model.AgentState
import com.example.model.AgentStepLog
import com.example.model.Language
import com.example.model.StepStatus
import com.example.model.Translations

@Composable
fun AgentStepperView(
    agentState: AgentState,
    language: Language,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("agent_stepper_view"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF7FBF9),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD3E7DE)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F6E56)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeviceHub,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.agentProgressTitle(language),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F6E56)
                        )
                    )
                }

                Text(
                    text = "LangGraph Pipeline",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF6B4A12),
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3ECE0))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            agentState.stepLogs.forEachIndexed { index, step ->
                StepItem(
                    step = step,
                    isLast = index == agentState.stepLogs.lastIndex
                )
            }

            // Display Branch Pill if gate decided
            if (agentState.branchTaken.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (agentState.shouldProceedToWeightEstimation) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                        .border(
                            1.dp,
                            if (agentState.shouldProceedToWeightEstimation) Color(0xFF81C784) else Color(0xFFFFB74D),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "↳ ${agentState.branchTaken}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (agentState.shouldProceedToWeightEstimation) Color(0xFF1B5E20) else Color(0xFFE65100),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    step: AgentStepLog,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon / Status indicator
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when (step.status) {
                        StepStatus.COMPLETED -> Color(0xFF2E7D32)
                        StepStatus.RUNNING -> Color(0xFF0F6E56)
                        StepStatus.FAILED -> Color(0xFFC62828)
                        StepStatus.SKIPPED -> Color(0xFF9E9E9E)
                        StepStatus.PENDING -> Color(0xFFE0E0E0)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                StepStatus.COMPLETED -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                StepStatus.RUNNING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
                StepStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                else -> {
                    val stepIcon = when (step.node) {
                        AgentNode.EXTRACT_EVIDENCE -> Icons.Default.Visibility
                        AgentNode.CONFIDENCE_GATE -> Icons.Default.DeviceHub
                        AgentNode.ESTIMATE_WEIGHT, AgentNode.LOW_CONFIDENCE_EXIT -> Icons.Default.Psychology
                        AgentNode.ASSEMBLE_OUTPUT -> Icons.Default.DataObject
                        else -> Icons.Default.Check
                    }
                    Icon(
                        imageVector = stepIcon,
                        contentDescription = null,
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (step.status == StepStatus.RUNNING) FontWeight.Bold else FontWeight.SemiBold,
                    color = when (step.status) {
                        StepStatus.RUNNING -> Color(0xFF0F6E56)
                        StepStatus.COMPLETED -> Color(0xFF1B5E20)
                        StepStatus.FAILED -> Color(0xFFC62828)
                        else -> Color(0xFF424242)
                    }
                )
            )

            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF616161),
                    fontSize = 11.sp
                )
            )

            if (!step.detail.isNullOrBlank()) {
                Text(
                    text = step.detail,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF0F6E56),
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
