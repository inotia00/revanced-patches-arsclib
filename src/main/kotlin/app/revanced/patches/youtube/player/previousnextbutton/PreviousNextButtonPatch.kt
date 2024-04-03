package app.revanced.patches.youtube.player.previousnextbutton

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.utils.fingerprints.PlayerControlsVisibilityModelFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc

@Suppress("unused")
object PreviousNextButtonPatch : BaseBytecodePatch(
    name = "Hide previous next button",
    description = "Adds an option to hide the previous and next buttons in the video player.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(PlayerControlsVisibilityModelFingerprint)
) {
    private const val HAS_NEXT = 5
    private const val HAS_PREVIOUS = 6

    private const val INTEGRATIONS_METHOD_REFERENCE =
        "$PLAYER_CLASS_DESCRIPTOR->hidePreviousNextButton(Z)Z"

    override fun execute(context: BytecodeContext) {

        PlayerControlsVisibilityModelFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val callIndex = it.scanResult.patternScanResult!!.endIndex
                val callInstruction = getInstruction<Instruction3rc>(callIndex)

                val hasNextParameterRegister = callInstruction.startRegister + HAS_NEXT
                val hasPreviousParameterRegister = callInstruction.startRegister + HAS_PREVIOUS

                addInstructions(
                    callIndex, """
                        invoke-static { v$hasNextParameterRegister }, $INTEGRATIONS_METHOD_REFERENCE
                        move-result v$hasNextParameterRegister
                        invoke-static { v$hasPreviousParameterRegister }, $INTEGRATIONS_METHOD_REFERENCE
                        move-result v$hasPreviousParameterRegister
                        """
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_PREVIOUS_NEXT_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("Hide previous next button")

    }
}
