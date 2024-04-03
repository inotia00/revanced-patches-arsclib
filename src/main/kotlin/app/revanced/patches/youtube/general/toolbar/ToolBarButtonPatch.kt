package app.revanced.patches.youtube.general.toolbar

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.toolbar.ToolBarHookPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object ToolBarButtonPatch : BaseBytecodePatch(
    name = "Hide toolbar button",
    description = "Adds an option to hide the button in the toolbar.",
    dependencies = setOf(
        SettingsPatch::class,
        ToolBarHookPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: BytecodeContext) {

        ToolBarHookPatch.injectCall("$GENERAL_CLASS_DESCRIPTOR->hideToolBarButton")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_TOOLBAR_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("Hide toolbar button")

    }
}
