package app.revanced.patches.youtube.flyoutpanel.oldqualitylayout

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.fingerprints.QualityMenuViewInflateFingerprint
import app.revanced.patches.youtube.utils.fingerprints.VideoQualitySetterFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.FLYOUT_PANEL
import app.revanced.patches.youtube.utils.recyclerview.BottomSheetRecyclerViewPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getReference
import app.revanced.util.getTargetIndex
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Patch(
    name = "Enable old quality layout",
    description = "Adds an option to restore the old video quality menu with specific video resolution options.",
    dependencies = [
        BottomSheetRecyclerViewPatch::class,
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39"
            ]
        )
    ]
)
@Suppress("unused")
object OldQualityLayoutPatch : BytecodePatch(
    setOf(
        QualityMenuViewInflateFingerprint,
        VideoQualitySetterFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Old method
         */
        val videoQualityClass = VideoQualitySetterFingerprint.result?.mutableMethod?.definingClass
            ?: throw VideoQualitySetterFingerprint.exception

        QualityMenuViewInflateFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "invoke-static { v$insertRegister }, $FLYOUT_PANEL->enableOldQualityMenu(Landroid/widget/ListView;)V"
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
                        invoke-static {}, $FLYOUT_PANEL->enableOldQualityMenu()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :show
                        """, ExternalLabel("show", getInstruction(jumpIndex))
                )
            } ?: throw PatchException("Failed to find onItemClick method")
        } ?: throw QualityMenuViewInflateFingerprint.exception

        /**
         * New method
         */
        BottomSheetRecyclerViewPatch.injectCall("$FLYOUT_PANEL->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")

        LithoFilterPatch.addFilter("$COMPONENTS_PATH/VideoQualityMenuFilter;")

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
