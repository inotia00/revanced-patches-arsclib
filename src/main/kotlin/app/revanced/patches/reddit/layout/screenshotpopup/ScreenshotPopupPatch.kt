package app.revanced.patches.reddit.layout.screenshotpopup

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.layout.screenshotpopup.fingerprints.ScreenshotTakenBannerFingerprint
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.getInstruction
import app.revanced.util.resultOrThrow

@Patch
@Name("Disable screenshot popup")
@Description("Adds an option to disable the popup that shows up when taking a screenshot.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Suppress("unused")
class ScreenshotPopupPatch : BytecodePatch(
    listOf(ScreenshotTakenBannerFingerprint)
) {
    companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "$PATCHES_PATH/ScreenshotPopupPatch;->disableScreenshotPopup()Z"
    }

    override fun execute(context: BytecodeContext) {

        ScreenshotTakenBannerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        invoke-static {}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        move-result v0
                        if-eqz v0, :dismiss
                        return-void
                        """, listOf(ExternalLabel("dismiss", getInstruction(0)))
                )
            }
        }

        updateSettingsStatus("enableScreenshotPopup")

    }
}
