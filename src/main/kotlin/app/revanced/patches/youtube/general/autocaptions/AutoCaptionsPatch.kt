package app.revanced.patches.youtube.general.autocaptions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.videoid.VideoIdPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object AutoCaptionsPatch : BaseBytecodePatch(
    name = "Disable auto captions",
    description = "Adds an option to disable captions from being automatically enabled.",
    dependencies = setOf(
        AutoCaptionsBytecodePatch::class,
        SettingsPatch::class,
        VideoIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: BytecodeContext) {

        VideoIdPatch.hookBackgroundPlayVideoId("$GENERAL_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: DISABLE_AUTO_CAPTIONS"
            )
        )

        SettingsPatch.updatePatchStatus("Disable auto captions")

    }
}