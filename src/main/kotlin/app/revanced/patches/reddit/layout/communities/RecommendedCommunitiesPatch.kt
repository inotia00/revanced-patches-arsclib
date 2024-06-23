package app.revanced.patches.reddit.layout.communities

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.layout.communities.fingerprints.CommunityRecommendationSectionFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.getInstruction
import app.revanced.util.resultOrThrow

@Patch
@Name("Hide recommended communities shelf")
@Description("Adds an option to hide the recommended communities shelves in subreddits.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Suppress("unused")
class RecommendedCommunitiesPatch : BytecodePatch(
    listOf(CommunityRecommendationSectionFingerprint)
) {
    companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "$PATCHES_PATH/RecommendedCommunitiesPatch;->hideRecommendedCommunitiesShelf()Z"
    }

    override fun execute(context: BytecodeContext) {

        CommunityRecommendationSectionFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructions(
                    0,
                    """
                        invoke-static {}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        move-result v0
                        if-eqz v0, :off
                        return-void
                        """, listOf(ExternalLabel("off", getInstruction(0)))
                )
            }
        }

        updateSettingsStatus("enableRecommendedCommunitiesShelf")

    }
}
