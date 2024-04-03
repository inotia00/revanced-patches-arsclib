package app.revanced.patches.youtube.misc.spoofappversion

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.copyXmlNode
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element

@Suppress("DEPRECATION", "unused")
object SpoofAppVersionPatch : BaseResourcePatch(
    name = "Spoof app version",
    description = "Adds options to spoof the YouTube client version. " +
            "This can be used to restore old UI elements and features.",
    dependencies = setOf(
        SettingsPatch::class,
        SpoofAppVersionBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        /**
         * Copy arrays
         */
        context.copyXmlNode("youtube/spoofappversion/host", "values/arrays.xml", "resources")

        if (SettingsPatch.upward1834) {
            context.appendChild(
                arrayOf(
                    "revanced_spoof_app_version_target_entry" to "@string/revanced_spoof_app_version_target_entry_18_33_40",
                    "revanced_spoof_app_version_target_entry_value" to "18.33.40",
                )
            )

            if (SettingsPatch.upward1839) {
                context.appendChild(
                    arrayOf(
                        "revanced_spoof_app_version_target_entry" to "@string/revanced_spoof_app_version_target_entry_18_38_45",
                        "revanced_spoof_app_version_target_entry_value" to "18.38.45"
                    )
                )

                if (SettingsPatch.upward1849) {
                    context.appendChild(
                        arrayOf(
                            "revanced_spoof_app_version_target_entry" to "@string/revanced_spoof_app_version_target_entry_18_48_39",
                            "revanced_spoof_app_version_target_entry_value" to "18.48.39"
                        )
                    )
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_APP_VERSION"
            )
        )

        SettingsPatch.updatePatchStatus("Spoof app version")

    }

    private fun ResourceContext.appendChild(entryArray: Array<Pair<String, String>>) {
        entryArray.map { (attributeName, attributeValue) ->
            this.xmlEditor["res/values/arrays.xml"].use { editor ->
                editor.file.apply {
                    val resourcesNode = getElementsByTagName("resources").item(0) as Element

                    val newElement: Element = createElement("item")
                    for (i in 0 until resourcesNode.childNodes.length) {
                        val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                        if (node.getAttribute("name") == attributeName) {
                            newElement.appendChild(createTextNode(attributeValue))
                            val firstChild = node.firstChild

                            node.insertBefore(newElement, firstChild)
                        }
                    }
                }
            }
        }
    }
}