package app.revanced.patches.youtube.seekbar.timestamps

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.seekbar.timestamps.fingerprints.TimeCounterFingerprint
import app.revanced.patches.youtube.utils.fingerprints.PlayerSeekbarColorFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.SEEKBAR_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object TimeStampPatch : BaseBytecodePatch(
    name = "Hide time stamp",
    description = "Adds an option to hide the timestamp in the bottom left of the video player.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(PlayerSeekbarColorFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PlayerSeekbarColorFingerprint.resultOrThrow().let { parentResult ->
            TimeCounterFingerprint.also { it.resolve(context, parentResult.classDef) }.resultOrThrow().let {
                it.mutableMethod.apply {
                    addInstructionsWithLabels(
                        0, """
                        invoke-static {}, $SEEKBAR_CLASS_DESCRIPTOR->hideTimeStamp()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(0))
                    )
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: HIDE_TIME_STAMP"
            )
        )

        SettingsPatch.updatePatchStatus("Hide time stamp")

    }
}
