package app.revanced.patches.youtube.player.infocards

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.player.infocards.fingerprints.InfoCardsIncognitoFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object InfoCardsPatch : BaseBytecodePatch(
    name = "Hide info cards",
    description = "Adds an option to hide info-cards in the video player.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(InfoCardsIncognitoFingerprint)
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/InfoCardsFilter;"

    override fun execute(context: BytecodeContext) {
        InfoCardsIncognitoFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister =
                    getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->hideInfoCard(Z)Z
                        move-result v$targetRegister
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
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_INFO_CARDS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide info cards")

    }
}