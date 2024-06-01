package app.revanced.patches.music.layout.branding.icon

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.patch.BaseResourcePatch
import java.io.File
import java.nio.file.Files

@Suppress("DEPRECATION", "unused")
object CustomBrandingIconPatch : BaseResourcePatch(
    name = "Custom branding icon YouTube Music",
    description = "Changes the YouTube Music app icon to the icon specified in options.json.",
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val DEFAULT_ICON_KEY = "Revancify Blue"

    private val availableIcon = mapOf(
        "MMT" to "mmt",
        DEFAULT_ICON_KEY to "revancify_blue",
        "Revancify Red" to "revancify_red",
        "AFN Blue" to "afn_blue",
        "AFN Red" to "afn_red"
    )

    private val sizeArray = arrayOf(
        "xxxhdpi",
        "xxhdpi",
        "xhdpi",
        "hdpi",
        "mdpi"
    )

    private val largeSizeArray = arrayOf(
        "xlarge-hdpi",
        "xlarge-mdpi",
        "large-xhdpi",
        "large-hdpi",
        "large-mdpi",
        "xxhdpi",
        "xhdpi",
        "hdpi",
        "mdpi",
    )

    private val drawableDirectories = sizeArray.map { "drawable-$it" }

    private val largeDrawableDirectories = largeSizeArray.map { "drawable-$it" }

    private val mipmapDirectories = sizeArray.map { "mipmap-$it" }

    private val headerIconResourceFileNames = arrayOf(
        "action_bar_logo",
        "logo_music",
        "ytm_logo"
    ).map { "$it.png" }.toTypedArray()

    private val launcherIconResourceFileNames = arrayOf(
        "adaptiveproduct_youtube_music_background_color_108",
        "adaptiveproduct_youtube_music_foreground_color_108",
        "ic_launcher_release"
    ).map { "$it.png" }.toTypedArray()

    private val splashIconResourceFileNames = arrayOf(
        // This file only exists in [drawable-hdpi]
        // Since {@code ResourceUtils#copyResources} checks for null values before copying,
        // Just adds it to the array.
        "action_bar_logo_release",
        "record"
    ).map { "$it.png" }.toTypedArray()

    private val headerIconResourceGroups = drawableDirectories.map { directory ->
        ResourceGroup(
            directory, *headerIconResourceFileNames
        )
    }

    private val launcherIconResourceGroups = mipmapDirectories.map { directory ->
        ResourceGroup(
            directory, *launcherIconResourceFileNames
        )
    }

    private val splashIconResourceGroups = largeDrawableDirectories.map { directory ->
        ResourceGroup(
            directory, *splashIconResourceFileNames
        )
    }

    private val AppIcon by stringPatchOption(
        key = "AppIcon",
        default = DEFAULT_ICON_KEY,
        values = availableIcon,
        title = "App icon",
        description = """
            The path to a folder containing the following folders:

            ${mipmapDirectories.joinToString("\n") { "- $it" }}

            Each of these folders has to have the following files:

            ${launcherIconResourceFileNames.joinToString("\n") { "- $it" }}
            """
            .split("\n")
            .joinToString("\n") { it.trimIndent() } // Remove the leading whitespace from each line.
            .trimIndent(), // Remove the leading newline.
    )

    private val ChangeSplashIcon by booleanPatchOption(
        key = "ChangeSplashIcon",
        default = true,
        title = "Change splash icons",
        description = "Apply the custom branding icon to the splash screen."
    )

    private val ChangeHeader by booleanPatchOption(
        key = "ChangeHeader",
        default = false,
        title = "Change header",
        description = "Apply the custom branding icon to the header."
    )

    override fun execute(context: ResourceContext) {
        AppIcon?.let { appIcon ->
            val appIconValue = appIcon.lowercase().replace(" ", "_")

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
                    } catch (_: Exception) {
                        // Exception is thrown if an invalid path is used in the patch option.
                        throw PatchException("Invalid app icon path: $appIcon")
                    }
                }
            } else {
                val resourcePath = "music/branding/$appIconValue"

                // Change launcher icon.
                launcherIconResourceGroups.let { resourceGroups ->
                    resourceGroups.forEach {
                        context.copyResources("$resourcePath/launcher", it)
                    }
                }

                // Change monochrome icon.
                arrayOf(
                    ResourceGroup(
                        "drawable",
                        "ic_app_icons_themed_youtube_music.xml"
                    )
                ).forEach { resourceGroup ->
                    context.copyResources("$resourcePath/monochrome", resourceGroup)
                }

                // Change header.
                if (ChangeHeader == true) {
                    headerIconResourceGroups.let { resourceGroups ->
                        resourceGroups.forEach {
                            context.copyResources("$resourcePath/header", it)
                        }
                    }
                }

                // Change splash icon.
                if (ChangeSplashIcon == true) {
                    splashIconResourceGroups.let { resourceGroups ->
                        resourceGroups.forEach {
                            context.copyResources("$resourcePath/splash", it)
                        }
                    }
                }
            }
        } ?: throw PatchException("Invalid app icon path.")
    }
}
