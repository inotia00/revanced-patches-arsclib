package app.revanced.patches.youtube.video.customspeed

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.flyoutpanel.oldspeedlayout.OldSpeedLayoutPatch
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.overridespeed.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object CustomPlaybackSpeedPatch : BaseResourcePatch(
    name = "Custom playback speed",
    description = "Adds an option to customize available playback speeds.",
    dependencies = setOf(
        CustomPlaybackSpeedBytecodePatch::class,
        OldSpeedLayoutPatch::class,
        OverrideSpeedHookPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: CUSTOM_PLAYBACK_SPEED"
            )
        )

        SettingsPatch.updatePatchStatus("Custom playback speed")
    }
}
