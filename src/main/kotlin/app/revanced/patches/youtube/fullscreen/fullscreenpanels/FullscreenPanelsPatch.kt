package app.revanced.patches.youtube.fullscreen.fullscreenpanels

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.fullscreen.fullscreenpanels.fingerprints.FullscreenEngagementPanelFingerprint
import app.revanced.patches.youtube.utils.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.FULLSCREEN_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.quickactions.QuickActionsHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FullScreenEngagementPanel
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object FullscreenPanelsPatch : BaseBytecodePatch(
    name = "Hide fullscreen panels",
    description = "Adds an option to hide panels such as live chat when in fullscreen.",
    dependencies = setOf(
        QuickActionsHookPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        FullscreenEngagementPanelFingerprint,
        LayoutConstructorFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        FullscreenEngagementPanelFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val literalIndex = getWideLiteralInstructionIndex(FullScreenEngagementPanel)
                val targetIndex = getTargetIndex(literalIndex, Opcode.CHECK_CAST)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $FULLSCREEN_CLASS_DESCRIPTOR->hideFullscreenPanels(Landroidx/coordinatorlayout/widget/CoordinatorLayout;)V"
                )
            }
        }

        LayoutConstructorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val dummyIndex = getWideLiteralInstructionIndex(159962)
                val dummyRegister = getInstruction<OneRegisterInstruction>(dummyIndex).registerA
                val addViewIndex = getTargetIndexWithMethodReferenceName("addView")

                addInstructionsWithLabels(
                    addViewIndex, """
                        invoke-static {}, $FULLSCREEN_CLASS_DESCRIPTOR->showFullscreenTitle()Z
                        move-result v$dummyRegister
                        if-eqz v$dummyRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(addViewIndex + 1))
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: HIDE_FULLSCREEN_PANELS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide fullscreen panels")

    }
}
