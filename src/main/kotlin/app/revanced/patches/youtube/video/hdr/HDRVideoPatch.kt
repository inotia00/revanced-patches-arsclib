package app.revanced.patches.youtube.video.hdr

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.hdr.fingerprints.HDRCapabilityFingerprint
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object HDRVideoPatch : BaseBytecodePatch(
    name = "Disable HDR video",
    description = "Adds options to disable HDR video.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(HDRCapabilityFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        HDRCapabilityFingerprint.result?.mutableMethod?.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $VIDEO_PATH/HDRVideoPatch;->disableHDRVideo()Z
                    move-result v0
                    if-nez v0, :default
                    return v0
                    """, ExternalLabel("default", getInstruction(0))
            )
        } ?: throw HDRCapabilityFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: DISABLE_HDR_VIDEO"
            )
        )

        SettingsPatch.updatePatchStatus("Disable HDR video")

    }
}
