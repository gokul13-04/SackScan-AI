package com.example.model

enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    TAMIL("ta", "தமிழ்");

    companion object {
        fun fromCode(code: String): Language =
            values().firstOrNull { it.code == code } ?: ENGLISH
    }
}

object Translations {
    fun appTitle(lang: Language) = when (lang) {
        Language.ENGLISH -> "SackScan AI"
        Language.TAMIL -> "சாக்கு ஸ்கேன் AI"
    }

    fun appSubtitle(lang: Language) = when (lang) {
        Language.ENGLISH -> "Smart Sack Weight Estimation"
        Language.TAMIL -> "புத்திசாலி சாக்கு எடை மதிப்பீடு"
    }

    fun optionalHintLabel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Optional hint"
        Language.TAMIL -> "விருப்ப குறிப்பு"
    }

    fun optionalHintPlaceholder(lang: Language) = when (lang) {
        Language.ENGLISH -> "Example: approximate sack size, if known"
        Language.TAMIL -> "எ.கா: தோராயமான சாக்கு அளவு, தெரிந்தால்"
    }

    fun cameraBtn(lang: Language) = when (lang) {
        Language.ENGLISH -> "Camera"
        Language.TAMIL -> "கேமரா"
    }

    fun uploadBtn(lang: Language) = when (lang) {
        Language.ENGLISH -> "Upload Image"
        Language.TAMIL -> "படம் பதிவேற்று"
    }

    fun scanSackBtn(lang: Language) = when (lang) {
        Language.ENGLISH -> "Scan Sack"
        Language.TAMIL -> "சாக்கு ஸ்கேன் செய்"
    }

    fun analyzingBtn(lang: Language) = when (lang) {
        Language.ENGLISH -> "Analyzing..."
        Language.TAMIL -> "பகுப்பாய்வு செய்கிறது..."
    }

    fun resetBtn(lang: Language) = when (lang) {
        Language.ENGLISH -> "Reset"
        Language.TAMIL -> "மீட்டமை"
    }

    fun estimatedWeightLabel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Estimated Weight"
        Language.TAMIL -> "மதிப்பிடப்பட்ட எடை"
    }

    fun weightRangeLabel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Weight Range"
        Language.TAMIL -> "எடை வரம்பு"
    }

    fun confidenceLabel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Overall Confidence"
        Language.TAMIL -> "ஒட்டுமொத்த நம்பகத்தன்மை"
    }

    fun dimensionsLabel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Approximate Dimensions (H × W × D)"
        Language.TAMIL -> "தோராயமான அளவுகள் (உ × அ × ஆ)"
    }

    fun volumeLabel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Filled Volume"
        Language.TAMIL -> "நிரப்பப்பட்ட கொள்ளளவு"
    }

    fun fillLevelLabel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Fill Level"
        Language.TAMIL -> "நிரப்பு நிலை"
    }

    fun conditionLabel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Condition"
        Language.TAMIL -> "சாக்கின் நிலை"
    }

    fun visualEvidenceLabel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Visual Evidence"
        Language.TAMIL -> "காட்சி சான்றுகள்"
    }

    fun summaryLabel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Summary"
        Language.TAMIL -> "சுருக்கம்"
    }

    fun technicalDetailsLabel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Technical Details"
        Language.TAMIL -> "தொழில்நுட்ப விவரங்கள்"
    }

    fun agentProgressTitle(lang: Language) = when (lang) {
        Language.ENGLISH -> "Agent Progress"
        Language.TAMIL -> "முகவர் செயல்முறை"
    }

    fun noImageSelected(lang: Language) = when (lang) {
        Language.ENGLISH -> "No sack image selected. Please take a photo or upload an image."
        Language.TAMIL -> "சாக்கு படம் தேர்ந்தெடுக்கப்படவில்லை. புகைப்படம் எடுக்கவும் அல்லது பதிவேற்றவும்."
    }

    fun scaleRefUsed(lang: Language) = when (lang) {
        Language.ENGLISH -> "Scale reference identified in frame"
        Language.TAMIL -> "சட்டகத்தில் அளவு குறிப்பு பொருள் அடையாளம் காணப்பட்டது"
    }

    fun scaleRefNotUsed(lang: Language) = when (lang) {
        Language.ENGLISH -> "No scale reference detected (-15% confidence)"
        Language.TAMIL -> "அளவு குறிப்பு பொருள் கண்டறியப்படவில்லை (-15% நம்பகத்தன்மை)"
    }

    fun clearImage(lang: Language) = when (lang) {
        Language.ENGLISH -> "Remove image"
        Language.TAMIL -> "படத்தை நீக்கு"
    }

    fun nodeExtractEvidence(lang: Language) = when (lang) {
        Language.ENGLISH -> "1. Extract Evidence"
        Language.TAMIL -> "1. சான்றுகளைப் பிரித்தெடுத்தல்"
    }

    fun nodeConfidenceGate(lang: Language) = when (lang) {
        Language.ENGLISH -> "2. Confidence Gate"
        Language.TAMIL -> "2. நம்பகத்தன்மை வாயில்"
    }

    fun nodeEstimateWeight(lang: Language) = when (lang) {
        Language.ENGLISH -> "3. Estimate Weight"
        Language.TAMIL -> "3. எடை மதிப்பீடு"
    }

    fun nodeLowConfidenceExit(lang: Language) = when (lang) {
        Language.ENGLISH -> "3. Low Confidence Exit"
        Language.TAMIL -> "3. குறைந்த நம்பகத்தன்மை முடிவு"
    }

    fun nodeAssembleOutput(lang: Language) = when (lang) {
        Language.ENGLISH -> "4. Assemble Output"
        Language.TAMIL -> "4. முடிவை தொகுத்தல்"
    }

    fun branchConfidencePassed(lang: Language) = when (lang) {
        Language.ENGLISH -> "Gate Passed: Sufficient visual evidence & volume > 0 -> Estimating Weight"
        Language.TAMIL -> "வாயில் கடந்தது: போதுமான காட்சி சான்றுகள் & கொள்ளளவு > 0 -> எடை மதிப்பீடு"
    }

    fun branchLowConfidence(lang: Language) = when (lang) {
        Language.ENGLISH -> "Gate Diverted: Insufficient evidence or no sack found -> Low Confidence Exit"
        Language.TAMIL -> "வாயில் திரும்பியது: போதிய சான்றுகள் இல்லை -> குறைந்த நம்பகத்தன்மை முடிவு"
    }

    fun lowConfidenceAdvice(lang: Language) = when (lang) {
        Language.ENGLISH -> "The weight cannot be responsibly estimated from this photo. Please retake the photo with the whole sack in frame, captured from the side, resting on a flat surface, ideally with a known reference object nearby."
        Language.TAMIL -> "இந்தப் புகைப்படத்திலிருந்து எடையை நம்பகத்தன்மையுடன் கணிக்க முடியாது. முழு சாக்கும் தெரியும்படி, பக்கவாட்டிலிருந்து, தட்டையான தரையில் இருக்கும்படி மீண்டும் படம் எடுக்கவும்."
    }

    fun unknown(lang: Language) = when (lang) {
        Language.ENGLISH -> "Unknown"
        Language.TAMIL -> "தெரியவில்லை"
    }

    fun apiKeySettings(lang: Language) = when (lang) {
        Language.ENGLISH -> "API Key Settings"
        Language.TAMIL -> "API சாவி அமைப்புகள்"
    }

    fun enterApiKey(lang: Language) = when (lang) {
        Language.ENGLISH -> "Gemini API Key"
        Language.TAMIL -> "ஜெமினி API சாவி"
    }

    fun saveApiKey(lang: Language) = when (lang) {
        Language.ENGLISH -> "Save Key"
        Language.TAMIL -> "சேமி"
    }

    fun cancel(lang: Language) = when (lang) {
        Language.ENGLISH -> "Cancel"
        Language.TAMIL -> "ரத்து"
    }

    fun densityNote(lang: Language) = when (lang) {
        Language.ENGLISH -> "Note: Assumed bulk density is applied as contents are intentionally not identified."
        Language.TAMIL -> "குறிப்பு: சாக்கினுள் உள்ள பொருள் திட்டமிட்டு கண்டறியப்படாததால், தோராய மொத்த அடர்த்தி பயன்படுத்தப்படுகிறது."
    }
}
