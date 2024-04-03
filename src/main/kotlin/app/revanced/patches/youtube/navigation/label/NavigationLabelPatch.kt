package app.revanced.patches.youtube.navigation.label

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.navigation.label.fingerprints.PivotBarSetTextFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.NAVIGATION_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
object NavigationLabelPatch : BaseBytecodePatch(
    name = "Hide navigation label",
    description = "Adds an option to hide navigation bar labels.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(PivotBarSetTextFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PivotBarSetTextFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithMethodReferenceName("setText")
                val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $NAVIGATION_CLASS_DESCRIPTOR->hideNavigationLabel(Landroid/widget/TextView;)V"
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_NAVIGATION_LABEL"
            )
        )

        SettingsPatch.updatePatchStatus("Hide navigation label")

    }
}
