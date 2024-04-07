package app.revanced.patches.music.flyoutpanel.replace

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.music.flyoutpanel.shared.FlyoutPanelMenuItemPatch
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.information.VideoInformationPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object ReplaceDismissQueuePatch : BaseBytecodePatch(
    name = "Replace dismiss queue",
    description = "Adds an option to replace \"Dismiss queue\" with \"Watch on YouTube\" in the flyout menu.",
    dependencies = setOf(
        FlyoutPanelMenuItemPatch::class,
        SettingsPatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: BytecodeContext) {
        FlyoutPanelMenuItemPatch.replaceComponents()

        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_panel_dismiss_queue",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_panel_dismiss_queue_continue_watch",
            "true",
            "revanced_replace_flyout_panel_dismiss_queue"
        )

    }
}
