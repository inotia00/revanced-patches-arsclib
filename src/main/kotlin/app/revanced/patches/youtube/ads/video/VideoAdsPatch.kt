package app.revanced.patches.youtube.ads.video

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object VideoAdsPatch : BaseResourcePatch(
    name = "Hide video ads",
    description = "Adds an option to hide ads in the video player.",
    dependencies = setOf(
        SettingsPatch::class,
        VideoAdsBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: ADS_SETTINGS",
                "SETTINGS: HIDE_VIDEO_ADS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide video ads")

    }
}
