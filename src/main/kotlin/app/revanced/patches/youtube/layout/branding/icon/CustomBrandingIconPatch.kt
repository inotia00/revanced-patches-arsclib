package app.revanced.patches.youtube.layout.branding.icon

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusIcon
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.patch.BaseResourcePatch
import java.io.File
import java.nio.file.Files

@Suppress("DEPRECATION", "unused")
object CustomBrandingIconPatch : BaseResourcePatch(
    name = "Custom branding icon YouTube",
    description = "Changes the YouTube app icon to the icon specified in options.json.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
) {
    private const val DEFAULT_ICON_KEY = "Revancify Blue"

    private val availableIcon = mapOf(
        "AFN Blue" to "afn_blue",
        "AFN Red" to "afn_red",
        "MMT" to "mmt",
        DEFAULT_ICON_KEY to "revancify_blue",
        "Revancify Red" to "revancify_red",
        "YouTube" to "youtube"
    )

    private val sizeArray = arrayOf(
        "xxxhdpi",
        "xxhdpi",
        "xhdpi",
        "hdpi",
        "mdpi"
    )

    private val drawableDirectories = sizeArray.map { "drawable-$it" }

    private val mipmapDirectories = sizeArray.map { "mipmap-$it" }

    private val headerIconResourceFileNames = arrayOf(
        "yt_premium_wordmark_header_dark",
        "yt_premium_wordmark_header_light",
        "yt_wordmark_header_dark",
        "yt_wordmark_header_light"
    ).map { "$it.png" }.toTypedArray()

    private val launcherIconResourceFileNames = arrayOf(
        "adaptiveproduct_youtube_background_color_108",
        "adaptiveproduct_youtube_foreground_color_108",
        "ic_launcher",
        "ic_launcher_round"
    ).map { "$it.png" }.toTypedArray()

    private val splashIconResourceFileNames = arrayOf(
        "product_logo_youtube_color_24",
        "product_logo_youtube_color_36",
        "product_logo_youtube_color_144",
        "product_logo_youtube_color_192"
    ).map { "$it.png" }.toTypedArray()

    private val oldSplashAnimationResourceFileNames = arrayOf(
        "\$\$avd_anim__1__0",
        "\$\$avd_anim__1__1",
        "\$\$avd_anim__2__0",
        "\$\$avd_anim__2__1",
        "\$\$avd_anim__3__0",
        "\$\$avd_anim__3__1",
        "\$avd_anim__0",
        "\$avd_anim__1",
        "\$avd_anim__2",
        "\$avd_anim__3",
        "\$avd_anim__4",
        "avd_anim"
    ).map { "$it.xml" }.toTypedArray()

    private fun List<String>.getResourceGroup(fileNames: Array<String>) = map { directory ->
        ResourceGroup(
            directory, *fileNames
        )
    }

    private val headerIconResourceGroups = drawableDirectories.getResourceGroup(headerIconResourceFileNames)

    private val launcherIconResourceGroups = mipmapDirectories.getResourceGroup(launcherIconResourceFileNames)

    private val splashIconResourceGroups = drawableDirectories.getResourceGroup(splashIconResourceFileNames)

    private val oldSplashAnimationResourceGroups = listOf("drawable").getResourceGroup(oldSplashAnimationResourceFileNames)

    // region patch option

    val AppIcon by stringPatchOption(
        key = "AppIcon",
        default = DEFAULT_ICON_KEY,
        values = availableIcon,
        title = "App icon",
        description = """
            The icon to apply to the app.
            
            If a path to a folder is provided, the folder must contain the following folders:

            ${mipmapDirectories.joinToString("\n") { "- $it" }}

            Each of these folders must contain the following files:

            ${launcherIconResourceFileNames.joinToString("\n") { "- $it" }}
            """.trimIndentMultiline(),
    )

    private val ChangeHeader by booleanPatchOption(
        key = "ChangeHeader",
        default = false,
        title = "Change header",
        description = "Apply the custom branding icon to the header."
    )

    private val ChangeSplashIcon by booleanPatchOption(
        key = "ChangeSplashIcon",
        default = true,
        title = "Change splash icons",
        description = "Apply the custom branding icon to the splash screen."
    )

    private val RestoreOldSplashAnimation by booleanPatchOption(
        key = "RestoreOldSplashAnimation",
        default = false,
        title = "Restore old splash animation",
        description = "Restores old style splash animation."
    )

    // endregion

    override fun execute(context: ResourceContext) {
        AppIcon?.let { appIcon ->
            val appIconValue = appIcon.lowercase().replace(" ", "_")
            val appIconResourcePath = "youtube/branding/$appIconValue"
            val stockResourcePath = "youtube/branding/stock"

            // Check if a custom path is used in the patch options.
            if (!availableIcon.containsValue(appIconValue)) {
                launcherIconResourceGroups.let { resourceGroups ->
                    try {
                        val path = File(appIcon)
                        val resourceDirectory = context["res"]

                        resourceGroups.forEach { group ->
                            val fromDirectory = path.resolve(group.resourceDirectoryName)
                            val toDirectory = resourceDirectory.resolve(group.resourceDirectoryName)

                            group.resources.forEach { iconFileName ->
                                Files.write(
                                    toDirectory.resolve(iconFileName).toPath(),
                                    fromDirectory.resolve(iconFileName).readBytes()
                                )
                            }
                        }

                        context.updatePatchStatusIcon("custom")
                    } catch (_: Exception) {
                        // Exception is thrown if an invalid path is used in the patch option.
                        throw PatchException("Invalid app icon path: $appIcon")
                    }
                }
            } else {
                // Change launcher icon.
                launcherIconResourceGroups.let { resourceGroups ->
                    resourceGroups.forEach {
                        context.copyResources("$appIconResourcePath/launcher", it)
                    }
                }

                // Change monochrome icon.
                arrayOf(
                    ResourceGroup(
                        "drawable",
                        "adaptive_monochrome_ic_youtube_launcher.xml"
                    )
                ).forEach { resourceGroup ->
                    context.copyResources("$appIconResourcePath/monochrome", resourceGroup)
                }

                // Change header.
                if (ChangeHeader == true) {
                    headerIconResourceGroups.let { resourceGroups ->
                        resourceGroups.forEach {
                            context.copyResources("$appIconResourcePath/header", it)
                        }
                    }
                }

                // Change splash icon.
                if (ChangeSplashIcon == true) {
                    splashIconResourceGroups.let { resourceGroups ->
                        resourceGroups.forEach {
                            context.copyResources("$appIconResourcePath/splash", it)
                        }
                    }
                }

                // Change splash screen.
                if (RestoreOldSplashAnimation == true) {
                    oldSplashAnimationResourceGroups.let { resourceGroups ->
                        resourceGroups.forEach {
                            context.copyResources("$stockResourcePath/splash", it)
                            context.copyResources("$appIconResourcePath/splash", it)
                        }
                    }

                    context.copyXmlNode("$stockResourcePath/splash", "values-v31/styles.xml", "resources")
                }

                context.updatePatchStatusIcon(appIconValue)
            }
        } ?: throw PatchException("Invalid app icon path.")
    }
}
