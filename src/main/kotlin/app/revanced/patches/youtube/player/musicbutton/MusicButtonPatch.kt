package app.revanced.patches.youtube.player.musicbutton

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.musicbutton.fingerprints.MusicAppDeeplinkButtonFingerprint
import app.revanced.patches.youtube.player.musicbutton.fingerprints.MusicAppDeeplinkButtonParentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object MusicButtonPatch : BaseBytecodePatch(
    name = "Hide music button",
    description = "Adds an option to hide the YouTube Music button in the video player.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(MusicAppDeeplinkButtonParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        val mutableClass = MusicAppDeeplinkButtonParentFingerprint.resultOrThrow().mutableClass
        MusicAppDeeplinkButtonFingerprint.resolve(context, mutableClass)

        MusicAppDeeplinkButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideMusicButton()Z
                        move-result v0
                        if-nez v0, :hidden
                        """,
                    ExternalLabel("hidden", getInstruction(implementation!!.instructions.size - 1))
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_YOUTUBE_MUSIC_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("Hide music button")

    }
}
