package app.revanced.patches.youtube.general.handle

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.general.handle.fingerprints.AccountSwitcherAccessibilityLabelFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AccountSwitcherAccessibility
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
object HandlePatch : BaseBytecodePatch(
    name = "Hide handle",
    description = "Adds options to hide the handle in the account switcher and You tab.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(AccountSwitcherAccessibilityLabelFingerprint)
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/HandlesFilter;"

    override fun execute(context: BytecodeContext) {

        AccountSwitcherAccessibilityLabelFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(AccountSwitcherAccessibility)
                val insertIndex = getTargetIndex(constIndex, Opcode.IF_EQZ)
                val setVisibilityIndex = getTargetIndexWithMethodReferenceName(insertIndex, "setVisibility")
                val visibilityRegister = getInstruction<FiveRegisterInstruction>(setVisibilityIndex).registerD

                addInstructions(
                    insertIndex, """
                        invoke-static {v$visibilityRegister}, $GENERAL_CLASS_DESCRIPTOR->hideHandle(I)I
                        move-result v$visibilityRegister
                        """
                )
            }
        }

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_HANDLE"
            )
        )

        SettingsPatch.updatePatchStatus("Hide handle")

    }
}
