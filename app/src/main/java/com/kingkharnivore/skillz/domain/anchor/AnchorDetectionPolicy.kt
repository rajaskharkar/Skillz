package com.kingkharnivore.skillz.domain.anchor

data class AnchorDetectionInput(
    val hasActiveFlow: Boolean,
    val flowRunning: Boolean,
    val flowPaused: Boolean,
    val globallyEnabled: Boolean,
    val enabledForFlow: Boolean,
    val disabledForFlow: Boolean,
    val anchorPaused: Boolean,
    val inBreak: Boolean,
    val usageAccessGranted: Boolean,
    val currentPackage: String?,
    val anchoredPackages: Set<String>,
    val neverAnchorPackages: Set<String>
)

object AnchorDetectionPolicy {
    fun shouldNudge(input: AnchorDetectionInput): Boolean {
        val pkg = input.currentPackage ?: return false
        val anchorAvailable = !input.disabledForFlow && (input.globallyEnabled || input.enabledForFlow)
        return input.hasActiveFlow &&
                input.flowRunning &&
                !input.flowPaused &&
                anchorAvailable &&
                !input.anchorPaused &&
                !input.inBreak &&
                input.usageAccessGranted &&
                pkg in input.anchoredPackages &&
                pkg !in input.neverAnchorPackages
    }
}

class AnchorEpisodeCounter {
    private var currentAnchorEpisodePackage: String? = null

    fun shouldCountEpisode(currentPackage: String?, anchoredPackages: Set<String>): Boolean {
        if (currentPackage == null || currentPackage !in anchoredPackages) {
            currentAnchorEpisodePackage = null
            return false
        }
        if (currentAnchorEpisodePackage == currentPackage) return false
        currentAnchorEpisodePackage = currentPackage
        return true
    }
}
