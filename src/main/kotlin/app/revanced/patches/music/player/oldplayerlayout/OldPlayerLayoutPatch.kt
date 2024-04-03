package app.revanced.patches.music.player.oldplayerlayout

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.music.player.oldplayerlayout.fingerprints.OldPlayerLayoutFingerprint
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object OldPlayerLayoutPatch : BaseBytecodePatch(
    name = "Enable old player layout",
    description = "Adds an option to return the player layout to the old style. Deprecated on YT Music 6.31.55+.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(OldPlayerLayoutFingerprint),
    use = false
) {
    override fun execute(context: BytecodeContext) {

        OldPlayerLayoutFingerprint.literalInstructionBooleanHook(
            45399578,
            "$PLAYER_CLASS_DESCRIPTOR->enableOldPlayerLayout(Z)Z",
            "Please use YT Music 6.29.58 or earlier."
        )

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_old_player_layout",
            "false"
        )

    }
}