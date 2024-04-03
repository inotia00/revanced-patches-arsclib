package app.revanced.patches.youtube.player.speedoverlay

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.RestoreSlideToSeekBehaviorFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object SpeedOverlayPatch : BaseBytecodePatch(
    name = "Disable speed overlay",
    description = "Adds an option to disable 'Play at 2x speed' when pressing and holding in the video player.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        RestoreSlideToSeekBehaviorFingerprint,
        SpeedOverlayFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        mapOf(
            RestoreSlideToSeekBehaviorFingerprint to 45411329,
            SpeedOverlayFingerprint to 45411330
        ).forEach { (fingerprint, literal) ->
            fingerprint.literalInstructionBooleanHook(
                literal,
                "$PLAYER_CLASS_DESCRIPTOR->disableSpeedOverlay(Z)Z"
            )
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: DISABLE_SPEED_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("Disable speed overlay")

    }
}