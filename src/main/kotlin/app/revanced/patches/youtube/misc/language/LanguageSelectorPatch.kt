package app.revanced.patches.youtube.misc.language

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.misc.language.fingerprints.GeneralPrefsFingerprint
import app.revanced.patches.youtube.misc.language.fingerprints.GeneralPrefsLegacyFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object LanguageSelectorPatch : BaseBytecodePatch(
    name = "Enable language switch",
    description = "Adds an option to enable or disable language switching toggle.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        GeneralPrefsFingerprint,
        GeneralPrefsLegacyFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        val result = GeneralPrefsFingerprint.result // YouTube v18.33.xx ~
            ?: GeneralPrefsLegacyFingerprint.result // ~ YouTube v18.32.xx
            ?: throw GeneralPrefsFingerprint.exception

        result.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex - 2
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {}, $MISC_PATH/LanguageSelectorPatch;->enableLanguageSwitch()Z
                        move-result v$insertRegister
                        """
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_LANGUAGE_SWITCH"
            )
        )

        SettingsPatch.updatePatchStatus("Enable language switch")

    }
}
