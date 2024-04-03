package app.revanced.patches.youtube.fullscreen.endscreenoverlay

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.fullscreen.endscreenoverlay.fingerprints.EndScreenResultsParentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.FULLSCREEN_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object EndScreenOverlayPatch : BaseBytecodePatch(
    name = "Hide end screen overlay",
    description = "Adds an option to hide the overlay in fullscreen when swiping up and at the end of videos.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(EndScreenResultsParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        EndScreenResultsParentFingerprint.result?.let {
            it.mutableClass.methods.find { method -> method.parameters == listOf("I", "Z", "I") }
                ?.apply {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $FULLSCREEN_CLASS_DESCRIPTOR->hideEndScreenOverlay()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """, ExternalLabel("show", getInstruction(0))
                    )
                } ?: throw PatchException("Could not find targetMethod")
        } ?: throw EndScreenResultsParentFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: HIDE_END_SCREEN_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("Hide end screen overlay")

    }
}
