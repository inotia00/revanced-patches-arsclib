package app.revanced.patches.youtube.general.descriptions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.general.descriptions.fingerprints.TextViewComponentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL
import app.revanced.patches.youtube.utils.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.recyclerview.BottomSheetRecyclerViewPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getTargetIndexWithMethodReferenceName
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch(
    name = "Description components",
    description = "Adds an option to hide or disable description components.",
    dependencies = [
        BottomSheetRecyclerViewPatch::class,
        LithoFilterPatch::class,
        SettingsPatch::class
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
object DescriptionComponentsPatch : BytecodePatch(
    setOf(TextViewComponentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        if (SettingsPatch.upward1902) {
            TextViewComponentFingerprint.result?.let {
                it.mutableMethod.apply {
                    val insertIndex = getTargetIndexWithMethodReferenceName("setTextIsSelectable")
                    val insertInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)

                    replaceInstruction(
                        insertIndex,
                        "invoke-static {v${insertInstruction.registerC}, v${insertInstruction.registerD}}, " +
                                "$GENERAL->disableDescriptionInteraction(Landroid/widget/TextView;Z)V"
                    )
                }
            } ?: throw TextViewComponentFingerprint.exception

            BottomSheetRecyclerViewPatch.injectCall("$GENERAL->onDescriptionPanelCreate(Landroid/support/v7/widget/RecyclerView;)V")

            /**
             * Add settings
             */
            SettingsPatch.addPreference(
                arrayOf(
                    "SETTINGS: DESCRIPTION_PANEL_INTERACTION"
                )
            )
        }

        LithoFilterPatch.addFilter("$COMPONENTS_PATH/DescriptionsFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: DESCRIPTION_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus("Description components")

    }
}
