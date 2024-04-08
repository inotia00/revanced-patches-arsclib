package app.revanced.patches.youtube.general.layout

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object LayoutComponentsPatch : BaseBytecodePatch(
    name = "Hide layout components",
    description = "Adds options to hide general layout components.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val CHANNEL_BAR_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/ChannelBarFilter;"
    private const val CUSTOM_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/CustomFilter;"
    private const val LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/LayoutComponentsFilter;"
    private const val KEYWORD_FILTER_CLASS_NAME =
        "$COMPONENTS_PATH/KeywordContentFilter;"

    override fun execute(context: BytecodeContext) {
        LithoFilterPatch.addFilter(CHANNEL_BAR_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(CUSTOM_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(KEYWORD_FILTER_CLASS_NAME)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                "PREFERENCE: GENERAL_SETTINGS",
                "PREFERENCE: PLAYER_SETTINGS",

                "SETTINGS: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: HIDE_AUDIO_TRACK_BUTTON",
                "SETTINGS: HIDE_CHANNEL_BAR_BUTTON",
                "SETTINGS: HIDE_LAYOUT_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide layout components")
    }
}
