package app.revanced.patches.reddit.ad.layout.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.ad.layout.annotations.LayoutAdsCompatibility

@Patch
@Name("layout-reddit-ads")
@Description("Removes layout ads from the Reddit frontpage and subreddits.")
@LayoutAdsCompatibility
@Version("0.0.1")
class LayoutAdsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {
        arrayOf(
            "height",
            "width"
        ).forEach { replacements ->
            context.xmlEditor[RESOURCE_FILE_PATH].use {
                val children = it.file.getElementsByTagName("merge").item(0).childNodes

                for (i in 1 until children.length) {
                    val view = children.item(i)
                    if (!(view.hasAttributes() && view.attributes.getNamedItem("android:id").nodeValue.endsWith("ad_view_stub"))) continue
                    view.attributes.getNamedItem("android:layout_$replacements").nodeValue = "0.0dip"
                    break
                }
            }
        }

        return PatchResultSuccess()
    }

    private companion object {
        const val RESOURCE_FILE_PATH = "res/layout/merge_listheader_link_detail.xml"
    }
}

