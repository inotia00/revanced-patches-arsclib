package app.revanced.patches.youtube.general.songsearch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patches.youtube.general.songsearch.fingerprints.VoiceSearchConfigFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object SongSearchPatch : BaseBytecodePatch(
    name = "Enable song search",
    description = "Adds an option to enable song search in the voice search screen.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(VoiceSearchConfigFingerprint),
    use = false
) {
    override fun execute(context: BytecodeContext) {

        VoiceSearchConfigFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->enableSongSearch()Z
                        move-result v0
                        return v0
                        """
                )
            }
        } ?: throw PatchException("This version is not supported. Please use YouTube 18.30.37 or later.")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: ENABLE_SONG_SEARCH"
            )
        )

        SettingsPatch.updatePatchStatus("Enable song search")
    }
}
