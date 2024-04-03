package app.revanced.patches.youtube.general.crowdfundingbox

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.general.crowdfundingbox.fingerprints.CrowdfundingBoxFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object CrowdfundingBoxPatch : BaseBytecodePatch(
    name = "Hide crowdfunding box",
    description = "Adds an option to hide the crowdfunding box between the player and video description.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(CrowdfundingBoxFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        CrowdfundingBoxFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->hideCrowdfundingBox(Landroid/view/View;)V"
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_CROWDFUNDING_BOX"
            )
        )

        SettingsPatch.updatePatchStatus("Hide crowdfunding box")

    }
}
