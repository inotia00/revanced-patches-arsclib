package app.revanced.patches.music.flyoutpanel.replace

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.flyoutpanel.shared.FlyoutPanelMenuItemPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.information.VideoInformationPatch

@Patch(
    name = "Replace dismiss queue",
    description = "Adds an option to replace \"Dismiss queue\" with \"Watch on YouTube\" in the flyout menu.",
    dependencies = [
        FlyoutPanelMenuItemPatch::class,
        SettingsPatch::class,
        VideoInformationPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object ReplaceDismissQueuePatch : BytecodePatch(emptySet()) {
    override fun execute(context: BytecodeContext) {
        FlyoutPanelMenuItemPatch.replaceComponents()

        SettingsPatch.addMusicPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_panel_dismiss_queue",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_panel_dismiss_queue_continue_watch",
            "true",
            "revanced_replace_flyout_panel_dismiss_queue"
        )

    }
}
