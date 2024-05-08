package app.revanced.patches.reddit.layout.branding.name

import app.revanced.patcher.ResourceContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.util.ResourceUtils.base
import app.revanced.util.ResourceUtils.setString

@Patch
@Name("Custom branding name reddit")
@Description("Renames the Reddit app to the name specified in options.json.")
@RedditCompatibility
@Suppress("unused")
class CustomBrandingNamePatch : ResourcePatch {
    companion object : OptionsContainer() {
        private const val ORIGINAL_APP_NAME = "Reddit"
        private const val APP_NAME = "RVX Reddit"

        private var AppName: String? by option(
            PatchOption.StringOption(
                key = "AppName",
                default = ORIGINAL_APP_NAME,
                title = "App name",
                description = "The name of the app."
            )
        )
    }

    override fun execute(context: ResourceContext) {
        val appName = if (AppName != null) {
            AppName!!
        } else {
            println("WARNING: Invalid name name. Does not apply patches.")
            ORIGINAL_APP_NAME
        }

        if (appName != ORIGINAL_APP_NAME) {
            context.base.setString("app_name", appName)
        } else {
            println("INFO: App name will remain unchanged as it matches the original.")
        }
    }
}
