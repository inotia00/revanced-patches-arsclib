package app.revanced.patches.reddit.utils.resourceid

import app.revanced.patcher.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.mapping.ResourceMappingPatch
import app.revanced.patches.shared.mapping.ResourceMappingPatch.Companion.getId
import app.revanced.patches.shared.mapping.ResourceType.ID

@DependsOn([ResourceMappingPatch::class])
class SharedResourceIdPatch : ResourcePatch {
    internal companion object {
        var ToolBarNavSearchCtaContainer = -1L
    }

    override fun execute(context: ResourceContext) {

        ToolBarNavSearchCtaContainer = getId(ID, "toolbar_nav_search_cta_container")

    }
}