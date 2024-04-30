package app.revanced.patches.music.general.castbutton

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patches.music.general.castbutton.fingerprints.MediaRouteButtonFingerprint
import app.revanced.patches.music.general.castbutton.fingerprints.PlayerOverlayChipFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.PlayerOverlayChip
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object CastButtonPatch : BaseBytecodePatch(
    name = "Hide cast button",
    description = "Adds an option to hide the cast button.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        MediaRouteButtonFingerprint,
        PlayerOverlayChipFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Hide cast button
         */
        MediaRouteButtonFingerprint.resultOrThrow().let {
            val setVisibilityMethod =
                it.mutableClass.methods.find { method -> method.name == "setVisibility" }

            setVisibilityMethod?.apply {
                addInstructions(
                    0, """
                        invoke-static {p1}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(I)I
                        move-result p1
                        """
                )
            } ?: throw PatchException("Failed to find setVisibility method")
        }

        /**
         * Hide floating cast banner
         */
        PlayerOverlayChipFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(PlayerOverlayChip) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(Landroid/view/View;)V"
                )
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_cast_button",
            "true"
        )

    }
}
