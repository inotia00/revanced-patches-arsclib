package app.revanced.patches.music.general.voicesearch

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.shared.voicesearch.VoiceSearchUtils.patchXml
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object VoiceSearchButtonPatch : BaseResourcePatch(
    name = "Hide voice search button",
    description = "Hides the voice search button in the search bar.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {

        context.patchXml(
            arrayOf("search_toolbar_view.xml"),
            arrayOf("height", "width")
        )

    }
}
