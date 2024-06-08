package app.revanced.patches.music.layout.branding.name

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.shared.elements.StringsElementsUtils.removeStringsElements
import app.revanced.util.patch.BaseResourcePatch

@Suppress("DEPRECATION", "unused")
object CustomBrandingNamePatch : BaseResourcePatch(
    name = "Custom branding name YouTube Music",
    description = "Renames the YouTube Music app to the name specified in options.json.",
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val APP_NAME_NOTIFICATION = "ReVanced Extended Music"
    private const val APP_NAME_LAUNCHER = "RVX Music"

    private val AppNameNotification by stringPatchOption(
        key = "AppNameNotification",
        default = APP_NAME_LAUNCHER,
        values = mapOf(
            "ReVanced Extended Music" to APP_NAME_NOTIFICATION,
            "RVX Music" to APP_NAME_LAUNCHER,
            "YouTube Music" to "YouTube Music",
            "YT Music" to "YT Music",
        ),
        title = "App name in notification panel",
        description = "The name of the app as it appears in the notification panel.",
    )

    private val AppNameLauncher by stringPatchOption(
        key = "AppNameLauncher",
        default = APP_NAME_LAUNCHER,
        values = mapOf(
            "ReVanced Extended Music" to APP_NAME_NOTIFICATION,
            "RVX Music" to APP_NAME_LAUNCHER,
            "YouTube Music" to "YouTube Music",
            "YT Music" to "YT Music",
        ),
        title = "App name in launcher",
        description = "The name of the app as it appears in the launcher.",
    )

    override fun execute(context: ResourceContext) {

        context.removeStringsElements(
            arrayOf("app_launcher_name", "app_name")
        )

        AppNameNotification?.let { notificationName ->
            AppNameLauncher?.let { launcherName ->
                context.xmlEditor["res/values/strings.xml"].use { editor ->
                    val document = editor.file

                    mapOf(
                        "app_name" to notificationName,
                        "app_launcher_name" to launcherName
                    ).forEach { (k, v) ->
                        val stringElement = document.createElement("string")

                        stringElement.setAttribute("name", k)
                        stringElement.textContent = v

                        document.getElementsByTagName("resources").item(0)
                            .appendChild(stringElement)
                    }
                }
            } ?: throw PatchException("Invalid launcher name.")
        } ?: throw PatchException("Invalid notification name.")
    }
}
