package app.revanced.patches.youtube.layout.branding.name

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.integrations.Constants.LANGUAGE_LIST
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusLabel
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import org.w3c.dom.Element
import kotlin.io.path.exists

@Patch(
    name = "Custom branding name YouTube",
    description = "Rename the YouTube app to the name specified in options.json.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43"
            ]
        )
    ]
)
@Suppress("unused")
object CustomBrandingNamePatch : ResourcePatch() {
    private const val APP_NAME = "ReVanced Extended"

    private val AppName by stringPatchOption(
        key = "AppName",
        default = APP_NAME,
        values = mapOf(
            "Full name" to APP_NAME,
            "Short name" to "RVX"
        ),
        title = "App name",
        description = "The name of the app.",
        required = true
    )

    override fun execute(context: ResourceContext) {

        AppName?.let {
            LANGUAGE_LIST.forEach { path ->
                val resDirectory = context["res"]
                val targetXmlPath = resDirectory.resolve(path).resolve("strings.xml").toPath()

                if (targetXmlPath.exists()) {
                    context.xmlEditor["res/$path/strings.xml"].use { editor ->
                        val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element

                        for (i in 0 until resourcesNode.childNodes.length) {
                            val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                            node.textContent = when (node.getAttribute("name")) {
                                "application_name" -> it

                                else -> continue
                            }
                        }
                    }
                }
            }
            context.updatePatchStatusLabel(it)
        } ?: throw PatchException("Invalid app name.")
    }
}
