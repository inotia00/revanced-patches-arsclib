package app.revanced.patches.reddit.misc.tracking.url

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.misc.tracking.url.fingerprints.ShareLinkFormatterFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.getInstruction
import app.revanced.util.resultOrThrow

@Patch
@Name("Sanitize sharing links")
@Description("Adds an option to remove tracking query parameters from URLs when sharing links.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Suppress("unused")
class SanitizeUrlQueryPatch : BytecodePatch(
    listOf(ShareLinkFormatterFingerprint)
) {
    companion object {
        private const val SANITIZE_METHOD_DESCRIPTOR =
            "$PATCHES_PATH/SanitizeUrlQueryPatch;->stripQueryParameters()Z"
    }

    override fun execute(context: BytecodeContext) {
        ShareLinkFormatterFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                addInstructions(
                    0,
                    """
                        invoke-static {}, $SANITIZE_METHOD_DESCRIPTOR
                        move-result v0
                        if-eqz v0, :off
                        return-object p0
                        """, listOf(ExternalLabel("off", getInstruction(0)))
                )
            }
        }

        updateSettingsStatus("enableSanitizeUrlQuery")

    }
}
