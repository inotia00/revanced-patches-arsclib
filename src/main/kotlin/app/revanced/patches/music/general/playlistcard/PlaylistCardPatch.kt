package app.revanced.patches.music.general.playlistcard

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object PlaylistCardPatch : BaseResourcePatch(
    name = "Hide playlist card",
    description = "Adds an option to hide the playlist card from the homepage.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/PlaylistCardFilter;"

    override fun execute(context: ResourceContext) {
        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_playlist_card",
            "false"
        )

    }
}
