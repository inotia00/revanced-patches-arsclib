package app.revanced.patches.youtube.buttomplayer.gestures

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.youtube.buttomplayer.gestures.fingerprints.BottomPlayerGesturesFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.BOTTOM_PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object BottomPlayerGesturesPatch : BaseBytecodePatch(
    name = "Enable bottom player gestures",
    description = "Adds an option to enter fullscreen when swiping down below the video player.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(BottomPlayerGesturesFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        BottomPlayerGesturesFingerprint.literalInstructionBooleanHook(
            45372793,
            "$BOTTOM_PLAYER_CLASS_DESCRIPTOR->enableBottomPlayerGestures()Z"
        )

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                "SETTINGS: ENABLE_BOTTOM_PLAYER_GESTURES"
            )
        )

        SettingsPatch.updatePatchStatus("Enable bottom player gestures")
    }
}
