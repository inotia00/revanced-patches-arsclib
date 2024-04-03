package app.revanced.patches.youtube.fullscreen.quickactions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.quickactions.QuickActionsHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object QuickActionsComponentsPatch : BaseBytecodePatch(
    name = "Quick actions components",
    description = "Adds options to hide and customize components below the seekbar in fullscreen.",
    dependencies = setOf(
        LithoFilterPatch::class,
        QuickActionsHookPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/QuickActionFilter;"

    override fun execute(context: BytecodeContext) {
        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        QuickActionsHookPatch.injectQuickActionMargin()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                "SETTINGS: QUICK_ACTIONS_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus("Quick actions components")

    }
}
