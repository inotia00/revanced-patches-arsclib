package app.revanced.patches.youtube.layout.voicesearch

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.shared.voicesearch.VoiceSearchUtils.patchXml
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object VoiceSearchButtonPatch : BaseResourcePatch(
    name = "Hide voice search button",
    description = "Hides the voice search button in the search bar.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {

        context.patchXml(
            arrayOf(
                "action_bar_search_results_view_mic.xml",
                "action_bar_search_view.xml",
                "action_bar_search_view_grey.xml",
                "action_bar_search_view_mic_out.xml"
            ),
            arrayOf(
                "height",
                "marginEnd",
                "marginStart",
                "width"
            )
        )

        SettingsPatch.updatePatchStatus("Hide voice search button")
    }
}
