package app.revanced.patches.youtube.player.suggestactions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.player.suggestactions.fingerprints.SuggestedActionsFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object SuggestedActionsPatch : BaseBytecodePatch(
    name = "Hide suggested actions",
    description = "Adds an option to hide the suggested actions bar inside the player.",
    dependencies = setOf(
        LithoFilterPatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(SuggestedActionsFingerprint)
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/SuggestedActionFilter;"

    override fun execute(context: BytecodeContext) {
        SuggestedActionsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $FILTER_CLASS_DESCRIPTOR->hideSuggestedActions(Landroid/view/View;)V"

                )
            }
        }

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_SUGGESTED_ACTION"
            )
        )

        SettingsPatch.updatePatchStatus("Hide suggested actions")

    }
}
