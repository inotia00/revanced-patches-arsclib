package app.revanced.patches.youtube.fullscreen.landscapemode

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.fullscreen.landscapemode.fingerprints.BroadcastReceiverFingerprint
import app.revanced.patches.youtube.fullscreen.landscapemode.fingerprints.LandScapeModeConfigFingerprint
import app.revanced.patches.youtube.fullscreen.landscapemode.fingerprints.OrientationParentFingerprint
import app.revanced.patches.youtube.fullscreen.landscapemode.fingerprints.OrientationPrimaryFingerprint
import app.revanced.patches.youtube.fullscreen.landscapemode.fingerprints.OrientationSecondaryFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.FULLSCREEN_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object LandScapeModePatch : BaseBytecodePatch(
    name = "Landscape mode",
    description = "Adds an option to disable landscape mode when entering fullscreen" +
            "and an option to keep landscape mode when turning the screen off and on in fullscreen.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        BroadcastReceiverFingerprint,
        LandScapeModeConfigFingerprint,
        OrientationParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        /**
         * Disable landscape mode
         */
        OrientationParentFingerprint.result?.classDef?.let { classDef ->
            arrayOf(
                OrientationPrimaryFingerprint,
                OrientationSecondaryFingerprint
            ).forEach { fingerprint ->
                fingerprint.resolve(context, classDef)

                fingerprint.result?.let {
                    it.mutableMethod.apply {
                        val index = it.scanResult.patternScanResult!!.endIndex
                        val register = getInstruction<OneRegisterInstruction>(index).registerA

                        addInstructions(
                            index + 1, """
                                invoke-static {v$register}, $FULLSCREEN_CLASS_DESCRIPTOR->disableLandScapeMode(Z)Z
                                move-result v$register
                                """
                        )
                    }
                } ?: throw fingerprint.exception
            }
        } ?: throw OrientationParentFingerprint.exception

        /**
         * Keep landscape mode
         */
        LandScapeModeConfigFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $FULLSCREEN_CLASS_DESCRIPTOR->keepFullscreen(Z)Z
                        move-result v$insertRegister
                        """
                )
            }

            BroadcastReceiverFingerprint.result?.let { result ->
                result.mutableMethod.apply {
                    val stringIndex = getStringInstructionIndex("android.intent.action.SCREEN_ON")
                    val insertIndex = getTargetIndex(stringIndex, Opcode.IF_EQZ) + 1

                    addInstruction(
                        insertIndex,
                        "invoke-static {}, $FULLSCREEN_CLASS_DESCRIPTOR->setScreenStatus()V"
                    )
                }
            } ?: throw BroadcastReceiverFingerprint.exception

            SettingsPatch.addPreference(
                arrayOf(
                    "SETTINGS: KEEP_LANDSCAPE_MODE"
                )
            )
        } // no exceptions are raised for compatibility with all versions.

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: FULLSCREEN_EXPERIMENTAL_FLAGS",
                "SETTINGS: DISABLE_LANDSCAPE_MODE"
            )
        )

        SettingsPatch.updatePatchStatus("Landscape mode")

    }
}
