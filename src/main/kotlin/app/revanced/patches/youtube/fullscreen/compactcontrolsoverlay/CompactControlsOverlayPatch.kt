package app.revanced.patches.youtube.fullscreen.compactcontrolsoverlay

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.FULLSCREEN_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getWalkerMethod
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object CompactControlsOverlayPatch : BaseBytecodePatch(
    name = "Enable compact controls overlay",
    description = "Adds an option to make the fullscreen controls compact.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(YouTubeControlsOverlayFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        YouTubeControlsOverlayFingerprint.resultOrThrow().let {
            val walkerMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex)
            walkerMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex,
                    """
                        invoke-static {v$targetRegister}, $FULLSCREEN_CLASS_DESCRIPTOR->enableCompactControlsOverlay(Z)Z
                        move-result v$targetRegister
                    """
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: FULLSCREEN_EXPERIMENTAL_FLAGS",
                "SETTINGS: ENABLE_COMPACT_CONTROLS_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("Enable compact controls overlay")

    }
}
