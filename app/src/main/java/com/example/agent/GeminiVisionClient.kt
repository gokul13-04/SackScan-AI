package com.example.agent

import android.util.Log
import com.example.model.Language
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ExtractedEvidence(
    val visualEvidence: String,
    val evidenceSufficient: Boolean,
    val condition: String,
    val approxHeightCm: Double,
    val approxWidthCm: Double,
    val approxDepthCm: Double,
    val approxVolumeLitres: Double,
    val fillLevelPercent: Int,
    val scaleReferenceUsed: Boolean,
    val rawJson: String,
    val modelUsed: String,
    val modelAttempts: List<String>
)

class GeminiVisionClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "SackScanAI"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        // Candidate models in fallback priority order
        val CANDIDATE_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-flash-latest",
            "gemini-2.0-flash",
            "gemini-3.5-flash",
            "gemini-1.5-flash"
        )

        val SYSTEM_PROMPT = """
You are SackScan AI, an objective physical geometry and bag analysis vision engine.
YOUR PRIMARY TASK:
Inspect the submitted photo of a sack/bag and extract physical geometrical cues only.

CRITICAL CONSTRAINTS (STRICTLY FORBIDDEN):
1. FORBIDDEN: DO NOT read, mention, use, or report any printed text, label, brand name, logo, barcode, or printed weight on the sack (even if "25 KG", "50 KG", "Basmati", or a brand is clearly visible). Pretend the entire sack exterior is completely blank and unprinted.
2. FORBIDDEN: DO NOT guess, speculate, mention, or report what is inside the sack (no rice, grain, sand, flour, seeds, cement, etc.). DO NOT include any "contents" or "material" field.
3. BASE YOUR ESTIMATE SOLELY ON PHYSICAL VISUAL EVIDENCE:
   - Proportions: aspect ratio of height, width, and depth in cm.
   - Bulge and roundness: cross-sectional curvature and belly protrusion.
   - Sag, slump, and folds: vertical gravitational settlement and wrinkle lines.
   - Seam tautness: tension along the perimeter stitches and corners.
   - How the base spreads against the supporting floor or pallet.
   - Indentation depth or pressure markings against the resting surface.
   - Any environmental reference objects in the scene (door frame, floor tiles, human foot, wall skirting, nearby containers) to estimate metric scale (cm).

OUTPUT FORMAT:
You MUST respond with a valid JSON object only, matching this exact structure:
{
  "visualEvidence": "<detailed description of visible physical cues: bulge, sag, seams, base spread, and resting pressure>",
  "evidenceSufficient": true/false,
  "condition": "<description of sack state: e.g. tightly packed, upright, moderately slumped, slack, etc.>",
  "approxHeightCm": <number in cm, or 0 if unknown>,
  "approxWidthCm": <number in cm, or 0 if unknown>,
  "approxDepthCm": <number in cm, or 0 if unknown>,
  "approxVolumeLitres": <estimated filled volume of the sack in litres based on 3D geometry, or 0 if unknown>,
  "fillLevelPercent": <integer 0-100 indicating percentage of capacity filled>,
  "scaleReferenceUsed": <true if nearby objects/ground grid were used for metric scaling, false if estimated from typical proportions alone>
}

If no sack-like container is present in the image, or if the view is too blurry/severely cropped to assess size or fill, set "evidenceSufficient": false and "approxVolumeLitres": 0. If a sack is present, set "evidenceSufficient": true.
        """.trimIndent()
    }

    suspend fun extractEvidence(
        base64Jpeg: String,
        optionalHint: String,
        language: Language,
        apiKey: String
    ): Result<ExtractedEvidence> {
        val attempts = mutableListOf<String>()
        var lastError: Exception? = null

        val langInstruction = if (language == Language.TAMIL) {
            """
CRITICAL LANGUAGE REQUIREMENT:
All descriptive text values in your JSON response (specifically "visualEvidence" and "condition") MUST be written fluently in TAMIL (தமிழ்).
However, ALL JSON keys must strictly remain in English as specified ("visualEvidence", "evidenceSufficient", "condition", "approxHeightCm", "approxWidthCm", "approxDepthCm", "approxVolumeLitres", "fillLevelPercent", "scaleReferenceUsed").
            """.trimIndent()
        } else {
            "Write all descriptive text values in English."
        }

        val userText = buildString {
            append(SYSTEM_PROMPT).append("\n\n")
            append("Analyze this sack/bag image for physical geometry, dimensions, and filled volume according to your instructions.")
            if (optionalHint.isNotBlank()) {
                append("\nOptional user hint: ").append(optionalHint.trim())
            }
            append("\n").append(langInstruction)
        }

        // Construct standard multimodal payload with responseMimeType "application/json"
        // and inline base64 image with mimeType "image/jpeg"
        val rootObj = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", SYSTEM_PROMPT)
                }))
            })

            val partsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("text", userText)
                })
                put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Jpeg)
                    })
                })
            }

            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", partsArray)
            }))

            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val requestBodyString = rootObj.toString()
        val requestBody = requestBodyString.toRequestBody("application/json; charset=utf-8".toMediaType())

        // Try candidate models sequentially until one succeeds
        for (model in CANDIDATE_MODELS) {
            try {
                Log.d(TAG, "Attempting Gemini Vision call using model: $model")
                println("SackScanAI: Attempting Gemini Vision call using model: $model")

                val endpoint = "$BASE_URL/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .build()

                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    httpClient.newCall(request).execute()
                }

                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errJson = JSONObject(responseBody)
                        errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $responseBody"
                    }
                    val failLog = "$model: $errorMsg"
                    attempts.add(failLog)
                    Log.w(TAG, "Model $model failed: $errorMsg. Trying next fallback...")
                    println("SackScanAI: Model $model failed: $errorMsg. Trying fallback...")
                    lastError = Exception(failLog)
                    continue
                }

                // Log raw response
                Log.d(TAG, "Raw response from $model: $responseBody")
                println("SackScanAI: Raw response from $model: $responseBody")

                // Extract candidate text
                val jsonRoot = JSONObject(responseBody)
                val candidates = jsonRoot.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    val failLog = "$model: No candidates returned"
                    attempts.add(failLog)
                    lastError = Exception(failLog)
                    continue
                }

                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

                Log.d(TAG, "Extracted text content from $model: $rawText")
                println("SackScanAI: Extracted text content from $model: $rawText")

                attempts.add("$model: Success")
                val parsedEvidence = parseEvidenceJson(rawText, language, model, attempts)
                return Result.success(parsedEvidence)

            } catch (e: Exception) {
                val failLog = "$model error: ${e.message}"
                attempts.add(failLog)
                Log.e(TAG, "Exception with model $model: ${e.message}", e)
                println("SackScanAI: Exception with model $model: ${e.message}")
                lastError = e
            }
        }

        return Result.failure(lastError ?: Exception("All Gemini models failed: ${attempts.joinToString("; ")}"))
    }

    /**
     * Tolerant JSON parsing:
     * - Strips markdown code fences
     * - Extracts substring from first '{' to last '}'
     * - Accepts both numbers and numeric strings for dimensions, volume, and fill level
     * - Matches field names exactly:
     *   visualEvidence, evidenceSufficient, condition, approxHeightCm,
     *   approxWidthCm, approxDepthCm, approxVolumeLitres, fillLevelPercent, scaleReferenceUsed
     */
    private fun parseEvidenceJson(
        rawText: String,
        language: Language,
        modelUsed: String,
        attempts: List<String>
    ): ExtractedEvidence {
        val cleanJson = extractJsonObjectString(rawText)
        val defaultUnknown = if (language == Language.TAMIL) "தெரியவில்லை" else "Unknown"

        return try {
            val json = JSONObject(cleanJson)

            val visualEvidence = json.optString("visualEvidence", "").ifBlank { defaultUnknown }
            val condition = json.optString("condition", "").ifBlank { defaultUnknown }
            val evidenceSufficient = parseToBoolean(json, "evidenceSufficient", default = true)
            val approxHeightCm = parseToDouble(json, "approxHeightCm", default = 0.0)
            val approxWidthCm = parseToDouble(json, "approxWidthCm", default = 0.0)
            val approxDepthCm = parseToDouble(json, "approxDepthCm", default = 0.0)
            val approxVolumeLitres = parseToDouble(json, "approxVolumeLitres", default = 0.0)
            val fillLevelPercent = parseToInt(json, "fillLevelPercent", default = 0)
            val scaleReferenceUsed = parseToBoolean(json, "scaleReferenceUsed", default = false)

            ExtractedEvidence(
                visualEvidence = visualEvidence,
                evidenceSufficient = evidenceSufficient,
                condition = condition,
                approxHeightCm = approxHeightCm,
                approxWidthCm = approxWidthCm,
                approxDepthCm = approxDepthCm,
                approxVolumeLitres = approxVolumeLitres,
                fillLevelPercent = fillLevelPercent,
                scaleReferenceUsed = scaleReferenceUsed,
                rawJson = cleanJson,
                modelUsed = modelUsed,
                modelAttempts = attempts
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON, creating fallback object: ${e.message}", e)
            ExtractedEvidence(
                visualEvidence = if (cleanJson.isNotBlank()) cleanJson else defaultUnknown,
                evidenceSufficient = true, // Graceful fallback
                condition = defaultUnknown,
                approxHeightCm = 0.0,
                approxWidthCm = 0.0,
                approxDepthCm = 0.0,
                approxVolumeLitres = 0.0,
                fillLevelPercent = 0,
                scaleReferenceUsed = false,
                rawJson = if (cleanJson.isNotBlank()) cleanJson else rawText,
                modelUsed = modelUsed,
                modelAttempts = attempts
            )
        }
    }

    private fun parseToDouble(json: JSONObject, key: String, default: Double): Double {
        if (!json.has(key)) return default
        val raw = json.opt(key) ?: return default
        if (raw is Number) return raw.toDouble()
        val str = raw.toString().replace(Regex("[^0-9.]"), "").trim()
        return str.toDoubleOrNull() ?: default
    }

    private fun parseToInt(json: JSONObject, key: String, default: Int): Int {
        if (!json.has(key)) return default
        val raw = json.opt(key) ?: return default
        if (raw is Number) return raw.toInt()
        val str = raw.toString().replace(Regex("[^0-9]"), "").trim()
        return str.toIntOrNull() ?: default
    }

    private fun parseToBoolean(json: JSONObject, key: String, default: Boolean): Boolean {
        if (!json.has(key)) return default
        val raw = json.opt(key) ?: return default
        if (raw is Boolean) return raw
        val str = raw.toString().trim().lowercase()
        return when (str) {
            "true", "yes", "1" -> true
            "false", "no", "0" -> false
            else -> default
        }
    }

    private fun extractJsonObjectString(text: String): String {
        var str = text.trim()
        // Strip markdown code fences
        if (str.startsWith("```")) {
            val firstLineBreak = str.indexOf('\n')
            if (firstLineBreak != -1) {
                str = str.substring(firstLineBreak + 1)
            }
            if (str.endsWith("```")) {
                str = str.substring(0, str.length - 3).trim()
            }
        }

        // Extract substring between first '{' and last '}'
        val startIdx = str.indexOf('{')
        val endIdx = str.lastIndexOf('}')
        if (startIdx != -1 && endIdx != -1 && endIdx >= startIdx) {
            return str.substring(startIdx, endIdx + 1)
        }

        return str
    }
}
