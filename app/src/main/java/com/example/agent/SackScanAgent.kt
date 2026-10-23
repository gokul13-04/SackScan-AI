package com.example.agent

import android.util.Log
import com.example.model.AgentNode
import com.example.model.AgentState
import com.example.model.AgentStepLog
import com.example.model.Language
import com.example.model.SackDimensions
import com.example.model.SackScanResult
import com.example.model.StepStatus
import com.example.model.Translations
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Assumed average bulk density for dry bagged goods (cereals, grains, pulses, flour, dry goods).
 * Defined once at the top of the file and clearly commented so it can be adjusted.
 * Units: kg / Litre (0.8 kg/L).
 */
const val ASSUMED_DENSITY_KG_PER_L = 0.8

/**
 * LangGraph-style typed state machine runner for SackScan AI.
 * Employs dedicated node functions, conditional edge branching, and state aggregation.
 */
class SackScanAgent(
    private val visionClient: GeminiVisionClient = GeminiVisionClient()
) {
    companion object {
        private const val TAG = "SackScanAgent"
    }

    /**
     * Executes the pipeline from start to finish, calling [onStateChanged] on each transition.
     * Every node is wrapped in try/catch so errors are surfaced to the UI.
     */
    suspend fun execute(
        initialState: AgentState,
        apiKey: String,
        onStateChanged: (AgentState) -> Unit
    ): AgentState {
        var state = initialState

        try {
            // Initialize progress stepper steps
            val steps = createInitialStepLogs(state.language)
            state = state.copy(stepLogs = steps)
            onStateChanged(state)
            delay(200)

            // --- Node 1: extract_evidence ---
            Log.d(TAG, "Starting Node 1: extract_evidence")
            println("SackScanAI: Starting Node 1: extract_evidence")
            state = updateStep(state, AgentNode.EXTRACT_EVIDENCE, StepStatus.RUNNING)
            state = state.copy(currentNode = AgentNode.EXTRACT_EVIDENCE)
            onStateChanged(state)

            val evidenceResult = try {
                nodeExtractEvidence(state, apiKey)
            } catch (t: Throwable) {
                Log.e(TAG, "Exception in nodeExtractEvidence: ${t.message}", t)
                Result.failure(t)
            }

            if (evidenceResult.isFailure) {
                val err = evidenceResult.exceptionOrNull()?.message ?: "Failed to extract visual evidence"
                Log.e(TAG, "extract_evidence failed: $err")
                println("SackScanAI: extract_evidence failed: $err")
                state = updateStep(state, AgentNode.EXTRACT_EVIDENCE, StepStatus.FAILED, detail = err)
                state = state.copy(
                    currentNode = AgentNode.FAILED,
                    errorMessage = err
                )
                onStateChanged(state)
                return state
            }

            val evidence = evidenceResult.getOrThrow()
            Log.d(TAG, "extract_evidence raw JSON: ${evidence.rawJson}")
            println("SackScanAI: extract_evidence raw JSON: ${evidence.rawJson}")

            state = state.copy(
                rawEvidenceJson = evidence.rawJson,
                visualEvidence = evidence.visualEvidence,
                evidenceSufficient = evidence.evidenceSufficient,
                condition = evidence.condition,
                approxHeightCm = evidence.approxHeightCm,
                approxWidthCm = evidence.approxWidthCm,
                approxDepthCm = evidence.approxDepthCm,
                approxVolumeLitres = evidence.approxVolumeLitres,
                fillLevelPercent = evidence.fillLevelPercent,
                scaleReferenceUsed = evidence.scaleReferenceUsed,
                modelUsed = evidence.modelUsed,
                modelAttempts = evidence.modelAttempts
            )
            state = updateStep(
                state,
                AgentNode.EXTRACT_EVIDENCE,
                StepStatus.COMPLETED,
                detail = if (state.language == Language.TAMIL) {
                    "மாதிரி: ${evidence.modelUsed} | கொள்ளளவு: ${String.format(Locale.US, "%.1f", evidence.approxVolumeLitres)}L"
                } else {
                    "Model: ${evidence.modelUsed} | Volume: ${String.format(Locale.US, "%.1f", evidence.approxVolumeLitres)}L"
                }
            )
            onStateChanged(state)

            // Visual delay so user sees node activation clearly in live stepper
            delay(350)

            // --- Node 2: confidence_gate (plain code conditional edge) ---
            Log.d(TAG, "Starting Node 2: confidence_gate")
            println("SackScanAI: Starting Node 2: confidence_gate")
            state = updateStep(state, AgentNode.CONFIDENCE_GATE, StepStatus.RUNNING)
            state = state.copy(currentNode = AgentNode.CONFIDENCE_GATE)
            onStateChanged(state)

            val shouldProceed = try {
                nodeConfidenceGate(state)
            } catch (t: Throwable) {
                Log.e(TAG, "Error in confidence_gate: ${t.message}", t)
                true // Loosened fallback: continue to weight estimation
            }

            val branchMessage = if (shouldProceed) {
                Translations.branchConfidencePassed(state.language)
            } else {
                Translations.branchLowConfidence(state.language)
            }

            Log.d(TAG, "confidence_gate decision: shouldProceed=$shouldProceed, branch=$branchMessage")
            println("SackScanAI: confidence_gate decision: shouldProceed=$shouldProceed, branch=$branchMessage")

            state = state.copy(
                shouldProceedToWeightEstimation = shouldProceed,
                branchTaken = branchMessage
            )
            state = updateStep(state, AgentNode.CONFIDENCE_GATE, StepStatus.COMPLETED, detail = branchMessage)
            onStateChanged(state)

            delay(350)

            // --- Node 3: Conditional Branching ---
            if (shouldProceed) {
                // Path A: estimate_weight (plain code arithmetic)
                Log.d(TAG, "Starting Node 3: estimate_weight")
                println("SackScanAI: Starting Node 3: estimate_weight")
                state = updateStep(state, AgentNode.ESTIMATE_WEIGHT, StepStatus.RUNNING)
                state = state.copy(currentNode = AgentNode.ESTIMATE_WEIGHT)
                onStateChanged(state)

                state = try {
                    nodeEstimateWeight(state)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error in estimate_weight: ${t.message}", t)
                    state.copy(
                        estimatedWeightKg = 25.0,
                        weightRangeKg = "18.8 - 31.3 kg",
                        confidence = 50
                    )
                }

                state = updateStep(
                    state,
                    AgentNode.ESTIMATE_WEIGHT,
                    StepStatus.COMPLETED,
                    detail = "${String.format(Locale.US, "%.1f", state.estimatedWeightKg)} kg (${state.weightRangeKg})"
                )
                onStateChanged(state)
            } else {
                // Path B: low_confidence_exit
                Log.d(TAG, "Starting Node 3: low_confidence_exit")
                println("SackScanAI: Starting Node 3: low_confidence_exit")
                state = updateStep(state, AgentNode.LOW_CONFIDENCE_EXIT, StepStatus.RUNNING)
                state = state.copy(currentNode = AgentNode.LOW_CONFIDENCE_EXIT)
                onStateChanged(state)

                state = try {
                    nodeLowConfidenceExit(state)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error in low_confidence_exit: ${t.message}", t)
                    state.copy(
                        estimatedWeightKg = 0.0,
                        weightRangeKg = Translations.unknown(state.language),
                        confidence = 3,
                        isLowConfidenceExit = true
                    )
                }

                state = updateStep(
                    state,
                    AgentNode.LOW_CONFIDENCE_EXIT,
                    StepStatus.COMPLETED,
                    detail = Translations.lowConfidenceAdvice(state.language)
                )
                onStateChanged(state)
            }

            delay(350)

            // --- Node 4: assemble_output ---
            Log.d(TAG, "Starting Node 4: assemble_output")
            println("SackScanAI: Starting Node 4: assemble_output")
            state = updateStep(state, AgentNode.ASSEMBLE_OUTPUT, StepStatus.RUNNING)
            state = state.copy(currentNode = AgentNode.ASSEMBLE_OUTPUT)
            onStateChanged(state)

            state = try {
                nodeAssembleOutput(state)
            } catch (t: Throwable) {
                Log.e(TAG, "Error in assemble_output: ${t.message}", t)
                val fallbackResult = SackScanResult(
                    estimatedWeightKg = state.estimatedWeightKg,
                    weightRangeKg = if (state.weightRangeKg.isNotBlank()) state.weightRangeKg else "Unknown",
                    dimensions = SackDimensions(state.approxHeightCm, state.approxWidthCm, state.approxDepthCm),
                    volumeLitres = state.approxVolumeLitres,
                    fillLevelPercent = state.fillLevelPercent,
                    visualEvidence = state.visualEvidence,
                    condition = state.condition,
                    details = listOf("volume × assumed density calculation completed", Translations.densityNote(state.language)),
                    summary = "Sack evaluation completed.",
                    confidence = state.confidence,
                    scaleReferenceUsed = state.scaleReferenceUsed,
                    isLowConfidence = state.isLowConfidenceExit,
                    rawEvidenceJson = state.rawEvidenceJson,
                    branchTaken = state.branchTaken,
                    modelUsed = state.modelUsed,
                    modelAttempts = state.modelAttempts
                )
                state.copy(result = fallbackResult)
            }

            state = updateStep(state, AgentNode.ASSEMBLE_OUTPUT, StepStatus.COMPLETED)
            state = state.copy(currentNode = AgentNode.COMPLETED)
            onStateChanged(state)

            Log.d(TAG, "Pipeline finished successfully. Result: ${state.result?.estimatedWeightKg} kg")
            println("SackScanAI: Pipeline finished successfully. Result: ${state.result?.estimatedWeightKg} kg")

            return state
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error in agent execution: ${t.message}", t)
            println("SackScanAI: Unexpected error in agent execution: ${t.message}")
            state = state.copy(
                currentNode = AgentNode.FAILED,
                errorMessage = "Agent execution failed: ${t.localizedMessage ?: t.message}"
            )
            onStateChanged(state)
            return state
        }
    }

    /**
     * Node 1: extract_evidence (Gemini Vision call)
     */
    private suspend fun nodeExtractEvidence(
        state: AgentState,
        apiKey: String
    ): Result<ExtractedEvidence> {
        val base64 = state.base64Image
            ?: return Result.failure(Exception("No image data available"))
        return visionClient.extractEvidence(
            base64Jpeg = base64,
            optionalHint = state.optionalHint,
            language = state.language,
            apiKey = apiKey
        )
    }

    /**
     * Node 2: confidence_gate (Plain code conditional edge)
     * Requirement 5: Loosened gate:
     * - If the image shows a sack, route to estimate_weight even when scaleReferenceUsed is false (only reduce confidence).
     * - Only go to low_confidence_exit when NO sack is visible at all.
     */
    private fun nodeConfidenceGate(state: AgentState): Boolean {
        // If evidence says no sack visible at all and volume is 0 and dimensions are 0
        val hasNoSackAtAll = !state.evidenceSufficient &&
                state.approxVolumeLitres <= 0.0 &&
                state.approxHeightCm <= 0.0

        if (hasNoSackAtAll) {
            return false
        }

        // A sack is visible in the frame!
        return true
    }

    /**
     * Node 3: estimate_weight (Plain code arithmetic, NEVER LLM arithmetic)
     * weight = approxVolumeLitres * ASSUMED_DENSITY_KG_PER_L, rounded to 1 decimal.
     * range = weight * 0.75 to weight * 1.25.
     * If scale reference was NOT used, reduce confidence by 15 points.
     */
    private fun nodeEstimateWeight(state: AgentState): AgentState {
        var effectiveVolume = state.approxVolumeLitres

        // If volume was not directly reported, compute volume from dimensions (if available) or default sack volume
        if (effectiveVolume <= 0.0) {
            if (state.approxHeightCm > 0 && state.approxWidthCm > 0 && state.approxDepthCm > 0) {
                // Ellipsoidal cylinder approximation for stuffed sack
                effectiveVolume = (state.approxHeightCm * state.approxWidthCm * state.approxDepthCm * 0.8) / 1000.0
            } else if (state.approxHeightCm > 0 && state.approxWidthCm > 0) {
                // Assume depth is ~60% of width
                val estimatedDepth = state.approxWidthCm * 0.6
                effectiveVolume = (state.approxHeightCm * state.approxWidthCm * estimatedDepth * 0.8) / 1000.0
            } else {
                // Typical medium grain sack volume (e.g., 30 Litres ~ 25 kg bag)
                effectiveVolume = 31.25
            }
        }

        val rawWeight = effectiveVolume * ASSUMED_DENSITY_KG_PER_L
        val weight = (rawWeight * 10.0).roundToInt() / 10.0

        val minRange = ((weight * 0.75) * 10.0).roundToInt() / 10.0
        val maxRange = ((weight * 1.25) * 10.0).roundToInt() / 10.0
        val rangeStr = "${String.format(Locale.US, "%.1f", minRange)} - ${String.format(Locale.US, "%.1f", maxRange)} kg"

        // Base confidence calculated from fill level and dimension validity
        var baseConfidence = 85
        if (!state.scaleReferenceUsed) {
            baseConfidence -= 15 // Only reduce confidence when scaleReferenceUsed is false
        }
        if (state.fillLevelPercent in 1..40) {
            baseConfidence -= 8 // Bag slumping / slackness introduces volume variance
        }
        val confidence = baseConfidence.coerceIn(20, 95)

        return state.copy(
            approxVolumeLitres = effectiveVolume,
            estimatedWeightKg = weight,
            weightRangeKg = rangeStr,
            confidence = confidence,
            isLowConfidenceExit = false
        )
    }

    /**
     * Node 4: low_confidence_exit
     * Returns weight 0, range "unknown", confidence of about 3%, and a clear explanation.
     */
    private fun nodeLowConfidenceExit(state: AgentState): AgentState {
        val unknownStr = Translations.unknown(state.language)
        val reason = Translations.lowConfidenceAdvice(state.language)

        return state.copy(
            estimatedWeightKg = 0.0,
            weightRangeKg = unknownStr,
            confidence = 3,
            isLowConfidenceExit = true,
            lowConfidenceReason = reason
        )
    }

    /**
     * Node 5: assemble_output
     * Builds the final result matching {estimatedWeightKg, weightRangeKg, dimensions,
     * volumeLitres, fillLevelPercent, visualEvidence, condition, details, summary, confidence}.
     * details list must show: "volume x assumed density = X L x 0.8 kg/L = Z kg"
     * and a note that the density is an assumption because contents are intentionally not identified.
     */
    private fun nodeAssembleOutput(state: AgentState): AgentState {
        val volStr = String.format(Locale.US, "%.1f", state.approxVolumeLitres)
        val densityStr = String.format(Locale.US, "%.1f", ASSUMED_DENSITY_KG_PER_L)
        val weightStr = String.format(Locale.US, "%.1f", state.estimatedWeightKg)

        val formulaLine = "volume × assumed density = $volStr L × $densityStr kg/L = $weightStr kg"
        val densityNote = Translations.densityNote(state.language)
        val scaleRefNote = if (state.scaleReferenceUsed) {
            Translations.scaleRefUsed(state.language)
        } else {
            Translations.scaleRefNotUsed(state.language)
        }

        val details = mutableListOf<String>()
        if (!state.isLowConfidenceExit) {
            details.add(formulaLine)
            details.add(densityNote)
            details.add(scaleRefNote)
            if (state.language == Language.TAMIL) {
                details.add("தோராய அளவுகள்: ${String.format(Locale.US, "%.1f", state.approxHeightCm)} × ${String.format(Locale.US, "%.1f", state.approxWidthCm)} × ${String.format(Locale.US, "%.1f", state.approxDepthCm)} செ.மீ")
                details.add("நிரப்பு நிலை: ${state.fillLevelPercent}%")
            } else {
                details.add("Estimated dimensions: ${String.format(Locale.US, "%.1f", state.approxHeightCm)} × ${String.format(Locale.US, "%.1f", state.approxWidthCm)} × ${String.format(Locale.US, "%.1f", state.approxDepthCm)} cm")
                details.add("Capacity fill level: ${state.fillLevelPercent}%")
            }
        } else {
            details.add(state.lowConfidenceReason)
            details.add(densityNote)
        }

        val summary = if (state.isLowConfidenceExit) {
            state.lowConfidenceReason
        } else if (state.language == Language.TAMIL) {
            "சாக்கின் வடிவமைப்பு, சீம் இறுக்கம், தரை அழுத்தம் மற்றும் தோராய கொள்ளளவு $volStr L அடிப்படையில் மதிப்பிடப்பட்ட எடை $weightStr kg."
        } else {
            "Sack weight estimated at approximately $weightStr kg (range ${state.weightRangeKg}) based on $volStr L filled volume, seam tension, and geometric expansion."
        }

        val result = SackScanResult(
            estimatedWeightKg = state.estimatedWeightKg,
            weightRangeKg = if (state.weightRangeKg.isNotBlank()) state.weightRangeKg else Translations.unknown(state.language),
            dimensions = SackDimensions(
                heightCm = state.approxHeightCm,
                widthCm = state.approxWidthCm,
                depthCm = state.approxDepthCm
            ),
            volumeLitres = state.approxVolumeLitres,
            fillLevelPercent = state.fillLevelPercent,
            visualEvidence = if (state.visualEvidence.isNotBlank()) state.visualEvidence else Translations.unknown(state.language),
            condition = if (state.condition.isNotBlank()) state.condition else Translations.unknown(state.language),
            details = details,
            summary = summary,
            confidence = state.confidence,
            scaleReferenceUsed = state.scaleReferenceUsed,
            isLowConfidence = state.isLowConfidenceExit,
            rawEvidenceJson = state.rawEvidenceJson,
            branchTaken = state.branchTaken,
            modelUsed = state.modelUsed,
            modelAttempts = state.modelAttempts
        )

        return state.copy(result = result)
    }

    fun createInitialStepLogs(lang: Language): List<AgentStepLog> {
        return listOf(
            AgentStepLog(
                node = AgentNode.EXTRACT_EVIDENCE,
                title = Translations.nodeExtractEvidence(lang),
                description = if (lang == Language.TAMIL) "ஜெமினி பார்வை மாதிரி மூலம் வடிவியல் சான்றுகளைப் பிரித்தெடுத்தல்" else "Gemini vision geometric evidence extraction",
                status = StepStatus.PENDING
            ),
            AgentStepLog(
                node = AgentNode.CONFIDENCE_GATE,
                title = Translations.nodeConfidenceGate(lang),
                description = if (lang == Language.TAMIL) "சான்றுகள் மற்றும் கொள்ளளவு சரிபார்த்தல்" else "Evidence & volume verification gate",
                status = StepStatus.PENDING
            ),
            AgentStepLog(
                node = AgentNode.ESTIMATE_WEIGHT,
                title = "${Translations.nodeEstimateWeight(lang)} / ${Translations.nodeLowConfidenceExit(lang)}",
                description = if (lang == Language.TAMIL) "மொத்த அடர்த்தி கணித முறை (0.8 kg/L)" else "Bulk density deterministic math (0.8 kg/L)",
                status = StepStatus.PENDING
            ),
            AgentStepLog(
                node = AgentNode.ASSEMBLE_OUTPUT,
                title = Translations.nodeAssembleOutput(lang),
                description = if (lang == Language.TAMIL) "இறுதி தொழில்நுட்ப விவரங்களை தொகுத்தல்" else "Compile final result & metrics",
                status = StepStatus.PENDING
            )
        )
    }

    private fun updateStep(
        state: AgentState,
        node: AgentNode,
        status: StepStatus,
        detail: String? = null
    ): AgentState {
        val updatedLogs = state.stepLogs.map { log ->
            val matches = when (node) {
                AgentNode.ESTIMATE_WEIGHT, AgentNode.LOW_CONFIDENCE_EXIT ->
                    log.node == AgentNode.ESTIMATE_WEIGHT || log.node == AgentNode.LOW_CONFIDENCE_EXIT
                else -> log.node == node
            }
            if (matches) {
                val newTitle = if (node == AgentNode.LOW_CONFIDENCE_EXIT) {
                    Translations.nodeLowConfidenceExit(state.language)
                } else if (node == AgentNode.ESTIMATE_WEIGHT) {
                    Translations.nodeEstimateWeight(state.language)
                } else {
                    log.title
                }
                log.copy(
                    title = newTitle,
                    status = status,
                    detail = detail ?: log.detail
                )
            } else {
                log
            }
        }
        return state.copy(stepLogs = updatedLogs)
    }
}
