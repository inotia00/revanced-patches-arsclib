package app.revanced.patches.youtube.video.customspeed

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.flyoutpanel.PlaybackSpeedFlyoutPanelHookPatch
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.youtube.utils.recyclerview.BottomSheetRecyclerViewPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object CustomPlaybackSpeedPatch : BaseResourcePatch(
    name = "Custom playback speed",
    description = "Adds an option to customize available playback speeds.",
    dependencies = setOf(
        CustomPlaybackSpeedBytecodePatch::class,
        BottomSheetRecyclerViewPatch::class,
        LithoFilterPatch::class,
        PlaybackSpeedFlyoutPanelHookPatch::class,
        SettingsPatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/PlaybackSpeedMenuFilter;"
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/CustomPlaybackSpeedPatch;"

    override fun execute(context: ResourceContext) {
        BottomSheetRecyclerViewPatch.injectCall("$INTEGRATIONS_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")
        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

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
