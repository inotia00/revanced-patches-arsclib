package app.revanced.patches.reddit.utils.settings

import app.revanced.patcher.ResourceContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.IntegrationsPatch
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.Companion.is_2025_40_or_greater
import app.revanced.util.ResourceUtils.base
import app.revanced.util.ResourceUtils.doRecursively
import app.revanced.util.ResourceUtils.valueOrThrow
import org.w3c.dom.Element

@Patch
@Name("Settings for Reddit")
@Description("Applies mandatory patches to implement RVX settings into the application.")
@DependsOn([IntegrationsPatch::class, SettingsBytecodePatch::class])
@RedditCompatibility
@RequiresIntegrations
class SettingsPatch : ResourcePatch {
    companion object : OptionsContainer() {
        private const val DEFAULT_NAME = "RVX"

        private var RVXSettingsMenuName = option(
            PatchOption.StringOption(
                key = "RVXSettingsMenuName",
                default = DEFAULT_NAME,
                title = "RVX settings menu name",
                description = "The name of the RVX settings menu.",
                required = true
            )
        )
    }

    override fun execute(context: ResourceContext) {
        /**
         * Replace settings icon and label
         */
        val settingsLabel = RVXSettingsMenuName
            .valueOrThrow()

        val newIcon = if (is_2025_40_or_greater) "icon_ai" else "icon_beta_planet"

        val xmlFiles = mutableListOf(
            "preferences.xml",
            "preferences_logged_in.xml",
        )

        if (!is_2025_40_or_greater) xmlFiles += "preferences_logged_in_old.xml"

        xmlFiles.forEach { targetXML ->
            val openFile = context.base.openFile("res/xml/$targetXML")
            if (openFile.exists) {
                openFile.close()

                context.base.openXmlFile("res/xml/$targetXML").use { editor ->
                    editor.file.doRecursively node@{ node ->
                        if (node !is Element) return@node

                        node.getAttributeNode("android:title")?.let { attribute ->
                            if (attribute.textContent == "@string/label_acknowledgements") {
                                attribute.textContent = settingsLabel
                                node.setAttribute("android:icon", "@drawable/$newIcon")
                            }
                        }
                    }
                }
            } else {
                openFile.close()
            }
        }

        SettingsBytecodePatch.updateSettingsLabel(settingsLabel)
    }
}
