package app.revanced.patches.reddit.ad.banner

import app.revanced.patcher.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.util.ResourceUtils.base

class BannerAdsPatch : ResourcePatch {
    companion object {
        private const val RESOURCE_FILE_PATH = "res/layout/merge_listheader_link_detail.xml"
    }
    override fun execute(context: ResourceContext) {
        context.base.openXmlFile(RESOURCE_FILE_PATH).use {
            it.file.getElementsByTagName("merge").item(0).childNodes.apply {
                val attributes = arrayOf("height", "width")

                for (i in 1 until length) {
                    val view = item(i)
                    if (
                        view.hasAttributes() &&
                        view.attributes.getNamedItem("android:id").nodeValue.endsWith("ad_view_stub")
                    ) {
                        attributes.forEach { attribute ->
                            view.attributes.getNamedItem("android:layout_$attribute").nodeValue =
                                "0.0dip"
                        }

                        break
                    }
                }
            }
        }

    }
}

