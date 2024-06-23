package app.revanced.patches.reddit.utils.settings

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.reddit.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.reddit.utils.settings.fingerprints.AcknowledgementsLabelBuilderFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.OssLicensesMenuActivityOnCreateFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.SettingsStatusLoadFingerprint
import app.revanced.patches.shared.settings.fingerprints.SharedSettingFingerprint
import app.revanced.util.getInstruction
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getTargetIndexWithMethodReferenceNameOrThrow
import app.revanced.util.resultOrThrow
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@RequiresIntegrations
class SettingsBytecodePatch : BytecodePatch(
    listOf(
        AcknowledgementsLabelBuilderFingerprint,
        OssLicensesMenuActivityOnCreateFingerprint,
        SharedSettingFingerprint,
        SettingsStatusLoadFingerprint,
    )
) {
    companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "$INTEGRATIONS_PATH/settings/ActivityHook;->initialize(Landroid/app/Activity;)V"

        private lateinit var settingsStatusLoadMethod: MutableMethod

        internal fun updateSettingsStatus(description: String) {
            settingsStatusLoadMethod.addInstruction(
                0,
                "invoke-static {}, $INTEGRATIONS_PATH/settings/SettingsStatus;->$description()V"
            )
        }
    }

    override fun execute(context: BytecodeContext) {

        /**
         * Set SharedPrefCategory
         */
        SharedSettingFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val stringIndex = getTargetIndexOrThrow(Opcode.CONST_STRING)
                val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA

                replaceInstruction(
                    stringIndex,
                    "const-string v$stringRegister, \"reddit_revanced\""
                )
            }
        }

        /**
         * Replace settings label
         */
        AcknowledgementsLabelBuilderFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex =
                    getTargetIndexWithMethodReferenceNameOrThrow("getString") + 2
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                addInstruction(
                    insertIndex,
                    "const-string v$insertRegister, \"ReVanced Extended\""
                )
            }
        }

        /**
         * Initialize settings activity
         */
        OssLicensesMenuActivityOnCreateFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex + 1

                addInstructions(
                    insertIndex, """
                        invoke-static {p0}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        return-void
                        """
                )
            }
        }

        settingsStatusLoadMethod = SettingsStatusLoadFingerprint.resultOrThrow().mutableMethod
    }
}