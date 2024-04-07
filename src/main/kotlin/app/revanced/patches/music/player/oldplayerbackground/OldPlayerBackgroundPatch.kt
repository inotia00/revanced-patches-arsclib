package app.revanced.patches.music.player.oldplayerbackground

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.music.player.oldplayerbackground.fingerprints.OldPlayerBackgroundFingerprint
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object OldPlayerBackgroundPatch : BaseBytecodePatch(
    name = "Enable old player background",
    description = "Adds an option to return the player background to the old style. Deprecated on YT Music 6.34.51+.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(OldPlayerBackgroundFingerprint),
    use = false
) {
    override fun execute(context: BytecodeContext) {

        OldPlayerBackgroundFingerprint.literalInstructionBooleanHook(
            45415319,
            "$PLAYER_CLASS_DESCRIPTOR->enableOldPlayerBackground(Z)Z",
            "Please use YT Music 6.33.52 or earlier."
        )

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_old_player_background",
            "false"
        )

    }
}