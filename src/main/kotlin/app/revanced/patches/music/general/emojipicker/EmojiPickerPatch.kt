package app.revanced.patches.music.general.emojipicker

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object EmojiPickerPatch : BaseResourcePatch(
    name = "Hide emoji picker and time stamp",
    description = "Adds an option to hide the emoji picker and time stamp when typing comments.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/EmojiPickerFilter;"

    override fun execute(context: ResourceContext) {
        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_hide_emoji_picker",
            "false"
        )

    }
}
