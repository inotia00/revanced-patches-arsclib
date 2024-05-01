package app.revanced.patches.youtube.player.descriptions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.player.descriptions.fingerprints.EngagementPanelTitleFingerprint
import app.revanced.patches.youtube.player.descriptions.fingerprints.EngagementPanelTitleParentFingerprint
import app.revanced.patches.youtube.player.descriptions.fingerprints.TextViewComponentFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.recyclerview.BottomSheetRecyclerViewPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getWalkerMethod
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object DescriptionComponentsPatch : BaseBytecodePatch(
    name = "Description components",
    description = "Adds an option to hide or disable description components.",
    dependencies = setOf(
        BottomSheetRecyclerViewPatch::class,
        LithoFilterPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        EngagementPanelTitleParentFingerprint,
        TextViewComponentFingerprint
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/DescriptionsFilter;"

    override fun execute(context: BytecodeContext) {

        // patch for disable video description interaction and expand video description.
        // since these patches are still A/B tested, they are classified as 'Experimental flags'.
        if (SettingsPatch.upward1902) {
            TextViewComponentFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = getTargetIndexWithMethodReferenceName("setTextIsSelectable")
                    val insertInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)

                    replaceInstruction(
                        insertIndex,
                        "invoke-static {v${insertInstruction.registerC}, v${insertInstruction.registerD}}, " +
                                "$PLAYER_CLASS_DESCRIPTOR->disableVideoDescriptionInteraction(Landroid/widget/TextView;Z)V"
                    )
                }
            }

            EngagementPanelTitleFingerprint.resolve(
                context,
                EngagementPanelTitleParentFingerprint.resultOrThrow().classDef
            )
            EngagementPanelTitleFingerprint.resultOrThrow().mutableMethod.apply {
                val contentDescriptionIndex = getTargetIndexWithMethodReferenceName("setContentDescription")
                val contentDescriptionRegister = getInstruction<FiveRegisterInstruction>(contentDescriptionIndex).registerD

                addInstruction(
                    contentDescriptionIndex,
                    "invoke-static {v$contentDescriptionRegister}," +
                            "$PLAYER_CLASS_DESCRIPTOR->setContentDescription(Ljava/lang/String;)V"
                )
            }

            BottomSheetRecyclerViewPatch.injectCall("$PLAYER_CLASS_DESCRIPTOR->onVideoDescriptionCreate(Landroid/support/v7/widget/RecyclerView;)V")

            /**
             * Add settings
             */
            SettingsPatch.addPreference(
                arrayOf(
                    "PREFERENCE_SCREEN: PLAYER",
                    "SETTINGS: DESCRIPTION_COMPONENTS",
                    "SETTINGS: DESCRIPTION_INTERACTION"
                )
            )
        }

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: DESCRIPTION_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
