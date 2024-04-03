package app.revanced.patches.youtube.flyoutpanel.oldqualitylayout

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.fingerprints.QualityMenuViewInflateFingerprint
import app.revanced.patches.youtube.utils.fingerprints.VideoQualitySetterFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.FLYOUT_PANEL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.recyclerview.BottomSheetRecyclerViewPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getReference
import app.revanced.util.getTargetIndex
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
object OldQualityLayoutPatch : BaseBytecodePatch(
    name = "Enable old quality layout",
    description = "Adds an option to restore the old video quality menu with specific video resolution options.",
    dependencies = setOf(
        BottomSheetRecyclerViewPatch::class,
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        QualityMenuViewInflateFingerprint,
        VideoQualitySetterFingerprint
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/VideoQualityMenuFilter;"

    override fun execute(context: BytecodeContext) {

        /**
         * Non-litho view, used in old clients and Shorts.
         */
        val videoQualityClass = VideoQualitySetterFingerprint.result?.mutableMethod?.definingClass
            ?: throw VideoQualitySetterFingerprint.exception

        QualityMenuViewInflateFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "invoke-static { v$insertRegister }, $FLYOUT_PANEL_CLASS_DESCRIPTOR->enableOldQualityMenu(Landroid/widget/ListView;)V"
                )
            }
            val onItemClickMethod =
                it.mutableClass.methods.find { method -> method.name == "onItemClick" }

            onItemClickMethod?.apply {
                val insertIndex = getTargetIndex(Opcode.IGET_OBJECT)
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                val jumpIndex = indexOfFirstInstruction {
                    opcode == Opcode.IGET_OBJECT
                            && this.getReference<FieldReference>()?.type == videoQualityClass
                }

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $FLYOUT_PANEL_CLASS_DESCRIPTOR->enableOldQualityMenu()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :show
                        """, ExternalLabel("show", getInstruction(jumpIndex))
                )
            } ?: throw PatchException("Failed to find onItemClick method")
        } ?: throw QualityMenuViewInflateFingerprint.exception

        /**
         * Litho view
         */
        BottomSheetRecyclerViewPatch.injectCall("$FLYOUT_PANEL_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FLYOUT_PANEL_SETTINGS",
                "SETTINGS: PLAYER_FLYOUT_PANEL_HEADER",
                "SETTINGS: ENABLE_OLD_QUALITY_LAYOUT"
            )
        )

        SettingsPatch.updatePatchStatus("Enable old quality layout")

    }
}
