package app.revanced.patches.youtube.player.endscreencards

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.player.endscreencards.fingerprints.LayoutCircleFingerprint
import app.revanced.patches.youtube.player.endscreencards.fingerprints.LayoutIconFingerprint
import app.revanced.patches.youtube.player.endscreencards.fingerprints.LayoutVideoFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object EndScreenCardsPatch : BaseBytecodePatch(
    name = "Hide end screen cards",
    description = "Adds an option to hide suggested video cards at the end of the video in the video player.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        LayoutCircleFingerprint,
        LayoutIconFingerprint,
        LayoutVideoFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        listOf(
            LayoutCircleFingerprint,
            LayoutIconFingerprint,
            LayoutVideoFingerprint
        ).forEach{ fingerprint ->
            fingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
                    val viewRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstruction(
                        insertIndex + 1,
                        "invoke-static { v$viewRegister }, $PLAYER_CLASS_DESCRIPTOR->hideEndScreenCards(Landroid/view/View;)V"
                    )
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_END_SCREEN_CARDS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide end screen cards")

    }
}
