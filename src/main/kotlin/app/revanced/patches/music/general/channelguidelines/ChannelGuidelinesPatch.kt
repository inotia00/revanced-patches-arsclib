package app.revanced.patches.music.general.channelguidelines

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object ChannelGuidelinesPatch : BaseResourcePatch(
    name = "Hide channel guidelines",
    description = "Adds an option to hide the channel guidelines at the top of the comments section.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/ChannelGuidelinesFilter;"

    override fun execute(context: ResourceContext) {
        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_hide_channel_guidelines",
            "true"
        )

    }
}
