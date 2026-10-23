package com.example.model

import android.graphics.Bitmap

enum class AgentNode {
    IDLE,
    EXTRACT_EVIDENCE,
    CONFIDENCE_GATE,
    ESTIMATE_WEIGHT,
    LOW_CONFIDENCE_EXIT,
    ASSEMBLE_OUTPUT,
    COMPLETED,
    FAILED
}

enum class StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}

data class AgentStepLog(
    val node: AgentNode,
    val title: String,
    val description: String,
    val status: StepStatus = StepStatus.PENDING,
    val detail: String? = null
)

data class SackDimensions(
    val heightCm: Double = 0.0,
    val widthCm: Double = 0.0,
    val depthCm: Double = 0.0
) {
    fun toDisplayString(): String {
        return if (heightCm > 0 || widthCm > 0 || depthCm > 0) {
            "${String.format("%.1f", heightCm)} × ${String.format("%.1f", widthCm)} × ${String.format("%.1f", depthCm)} cm"
        } else {
            "Unknown"
        }
    }
}

data class SackScanResult(
    val estimatedWeightKg: Double = 0.0,
    val weightRangeKg: String = "Unknown",
    val dimensions: SackDimensions = SackDimensions(),
    val volumeLitres: Double = 0.0,
    val fillLevelPercent: Int = 0,
    val visualEvidence: String = "Unknown",
    val condition: String = "Unknown",
    val details: List<String> = emptyList(),
    val summary: String = "",
    val confidence: Int = 0,
    val scaleReferenceUsed: Boolean = false,
    val isLowConfidence: Boolean = false,
    // Debug panel properties
    val rawEvidenceJson: String = "",
    val branchTaken: String = "",
    val modelUsed: String = "",
    val modelAttempts: List<String> = emptyList()
)

data class AgentState(
    val bitmap: Bitmap? = null,
    val base64Image: String? = null,
    val optionalHint: String = "",
    val language: Language = Language.ENGLISH,
    val currentNode: AgentNode = AgentNode.IDLE,
    val stepLogs: List<AgentStepLog> = emptyList(),
    // Evidence extraction:
    val rawEvidenceJson: String = "",
    val visualEvidence: String = "",
    val evidenceSufficient: Boolean = false,
    val condition: String = "",
    val approxHeightCm: Double = 0.0,
    val approxWidthCm: Double = 0.0,
    val approxDepthCm: Double = 0.0,
    val approxVolumeLitres: Double = 0.0,
    val fillLevelPercent: Int = 0,
    val scaleReferenceUsed: Boolean = false,
    val modelUsed: String = "",
    val modelAttempts: List<String> = emptyList(),
    // Gate decision:
    val branchTaken: String = "",
    val shouldProceedToWeightEstimation: Boolean = false,
    // Weight estimation:
    val estimatedWeightKg: Double = 0.0,
    val weightRangeKg: String = "",
    val confidence: Int = 0,
    val isLowConfidenceExit: Boolean = false,
    val lowConfidenceReason: String = "",
    // Final assembled output:
    val result: SackScanResult? = null,
    val errorMessage: String? = null
)
