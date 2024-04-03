package app.revanced.patches.youtube.general.castbutton

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patches.youtube.general.castbutton.fingerprints.CastButtonFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object CastButtonPatch : BaseBytecodePatch(
    name = "Hide cast button",
    description = "Adds an option to hide the cast button.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(CastButtonFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        CastButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        invoke-static {p1}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(I)I
                        move-result p1
                        """
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_CAST_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("Hide cast button")

    }
}
