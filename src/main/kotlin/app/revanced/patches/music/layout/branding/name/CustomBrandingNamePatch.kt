package app.revanced.patches.music.layout.branding.name

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.music.utils.integrations.Constants.LANGUAGE_LIST
import org.w3c.dom.Element
import kotlin.io.path.exists

@Patch(
    name = "Custom branding name YouTube Music",
    description = "Renames the YouTube Music app to the name specified in options.json.",
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object CustomBrandingNamePatch : ResourcePatch() {
    private const val APP_NAME_NOTIFICATION = "ReVanced Extended Music"
    private const val APP_NAME_LAUNCHER = "RVX Music"

    private val AppNameNotification by stringPatchOption(
        key = "AppNameNotification",
        default = APP_NAME_NOTIFICATION,
        values = mapOf(
            "Full name" to APP_NAME_NOTIFICATION,
            "Short name" to APP_NAME_LAUNCHER
        ),
        title = "App name in notification panel",
        description = "The name of the app as it appears in the notification panel.",
        required = true
    )

    private val AppNameLauncher by stringPatchOption(
        key = "AppNameLauncher",
        default = APP_NAME_LAUNCHER,
        values = mapOf(
            "Full name" to APP_NAME_NOTIFICATION,
            "Short name" to APP_NAME_LAUNCHER
        ),
        title = "App name in launcher",
        description = "The name of the app as it appears in the launcher.",
        required = true
    )

    override fun execute(context: ResourceContext) {

        val notificationName = AppNameNotification
            ?: throw PatchException("Invalid notification app name.")

        val launcherName = AppNameLauncher
            ?: throw PatchException("Invalid launcher app name.")

        LANGUAGE_LIST.forEach { path ->
            val resDirectory = context["res"]
            val targetXmlPath = resDirectory.resolve(path).resolve("strings.xml").toPath()

            if (targetXmlPath.exists()) {
                context.xmlEditor["res/$path/strings.xml"].use { editor ->
                    val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element

                    for (i in 0 until resourcesNode.childNodes.length) {
                        val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                        node.textContent = when (node.getAttribute("name")) {
                            "app_launcher_name" -> launcherName
                            "app_name" -> notificationName

                            else -> continue
                        }
                    }
                }
            }
        }
    }
}
