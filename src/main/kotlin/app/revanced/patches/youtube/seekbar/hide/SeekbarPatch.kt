package app.revanced.patches.youtube.seekbar.hide

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.seekbar.color.SeekbarColorPatch
import app.revanced.patches.youtube.utils.fingerprints.SeekbarFingerprint
import app.revanced.patches.youtube.utils.fingerprints.SeekbarOnDrawFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.SEEKBAR_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object SeekbarPatch : BaseBytecodePatch(
    name = "Hide seekbar",
    description = "Adds an option to hide the seekbar in video player and video thumbnails.",
    dependencies = setOf(
        SeekbarColorPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(SeekbarFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        SeekbarFingerprint.result?.mutableClass?.let { mutableClass ->
            SeekbarOnDrawFingerprint.also { it.resolve(context, mutableClass) }.result?.let {
                it.mutableMethod.apply {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $SEEKBAR_CLASS_DESCRIPTOR->hideSeekbar()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """, ExternalLabel("show", getInstruction(0))
                    )
                }
            } ?: throw SeekbarOnDrawFingerprint.exception
        } ?: throw SeekbarFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: HIDE_SEEKBAR"
            )
        )

        SettingsPatch.updatePatchStatus("Hide seekbar")

    }
}
