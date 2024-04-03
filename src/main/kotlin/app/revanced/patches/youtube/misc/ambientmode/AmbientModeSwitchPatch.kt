package app.revanced.patches.youtube.misc.ambientmode

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.AmbientModeInFullscreenFingerprint
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.PowerSaveModeFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.FULLSCREEN_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getTargetIndexWithMethodReferenceNameReversed
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object AmbientModeSwitchPatch : BaseBytecodePatch(
    name = "Ambient mode switch",
    description = "Adds an option to bypass the restrictions of ambient mode or disable it completely.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AmbientModeInFullscreenFingerprint,
        PowerSaveModeFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        PowerSaveModeFingerprint.result?.let {
            it.mutableMethod.apply {
                val powerSaveModePrimaryIndex = getTargetIndexWithMethodReferenceNameReversed("isPowerSaveMode")
                val powerSaveModeSecondaryIndex = getTargetIndexWithMethodReferenceNameReversed(powerSaveModePrimaryIndex - 1, "isPowerSaveMode")

                arrayOf(
                    powerSaveModePrimaryIndex,
                    powerSaveModeSecondaryIndex
                ).forEach { index ->
                    hook(index)
                }
            }
        } ?: throw PowerSaveModeFingerprint.exception

        AmbientModeInFullscreenFingerprint.literalInstructionBooleanHook(
            45389368,
            "$FULLSCREEN_CLASS_DESCRIPTOR->disableAmbientMode()Z"
        )

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: AMBIENT_MODE_SWITCH"
            )
        )

        SettingsPatch.updatePatchStatus("Ambient mode switch")

    }

    private fun MutableMethod.hook(index: Int) {
        val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

        addInstructions(
            index + 2, """
                invoke-static {v$register}, $MISC_PATH/AmbientModePatch;->bypassPowerSaveModeRestrictions(Z)Z
                move-result v$register
                """
        )
    }
}