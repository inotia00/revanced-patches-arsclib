package app.revanced.patches.youtube.player.suggestedvideoendscreen

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.suggestedvideoendscreen.fingerprints.RemoveOnLayoutChangeListenerFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getWalkerMethod
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
object SuggestedVideoEndScreenPatch : BaseBytecodePatch(
    name = "Fix suggested video end screen",
    description = "Fixes an issue where the suggested video end screen is shown at the end of a video, regardless of whether autoplay setting is on or off.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(RemoveOnLayoutChangeListenerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        /**
         * The reasons why this patch is classified as a patch that fixes a 'bug' are as follows:
         * 1. In YouTube v18.29.38, the suggested video end screen was only shown when the autoplay setting was turned on.
         * 2. Starting from YouTube v18.35.36, the suggested video end screen is shown regardless of whether autoplay setting was turned on or off.
         *
         * This patch changes the suggested video end screen to be shown only when the autoplay setting is turned on.
         * Automatically closing the suggested video end screen is not appropriate as it will disable the autoplay behavior.
         */
        RemoveOnLayoutChangeListenerFingerprint.resultOrThrow().let {
            val walkerIndex = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.endIndex)

            walkerIndex.apply {
                val invokeInterfaceIndex = getTargetIndex(Opcode.INVOKE_INTERFACE)
                val iGetObjectIndex = getTargetIndexReversed(invokeInterfaceIndex, Opcode.IGET_OBJECT)

                val invokeInterfaceReference = getInstruction<ReferenceInstruction>(invokeInterfaceIndex).reference
                val iGetObjectReference = getInstruction<ReferenceInstruction>(iGetObjectIndex).reference

                addInstructionsWithLabels(
                    0,
                    """
                        iget-object v0, p0, $iGetObjectReference

                        # This reference checks whether autoplay is turned on.
                        invoke-interface {v0}, $invokeInterfaceReference
                        move-result v0

                        # Hide suggested video end screen only when autoplay is turned off.
                        if-nez v0, :show_suggested_video_end_screen
                        return-void
                        """,
                    ExternalLabel(
                        "show_suggested_video_end_screen",
                        getInstruction(0)
                    )
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: PLAYER_EXPERIMENTAL_FLAGS",
                "SETTINGS: HIDE_SUGGESTED_VIDEO_END_SCREEN"
            )
        )

        SettingsPatch.updatePatchStatus("Fix suggested video end screen")

    }
}