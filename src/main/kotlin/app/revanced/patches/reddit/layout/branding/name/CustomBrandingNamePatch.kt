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
import app.revanced.util.ResourceUtils.valueOrThrow

@Patch
@Name("Custom branding name for Reddit")
@Description("Renames the Reddit app to the name specified in options.json.")
@RedditCompatibility
@Suppress("unused")
class CustomBrandingNamePatch : ResourcePatch {
    companion object : OptionsContainer() {
        private const val ORIGINAL_APP_NAME = "Reddit"
        private const val APP_NAME = "RVX Reddit"

        private var AppName = option(
            PatchOption.StringOption(
                key = "AppName",
                default = ORIGINAL_APP_NAME,
                title = "App name",
                description = "The name of the app.",
                required = true
            )
        )
    }

    override fun execute(context: ResourceContext) {
        val appName = AppName
            .valueOrThrow()

        if (appName == ORIGINAL_APP_NAME) {
            println("INFO: App name will remain unchanged as it matches the original.")
            return
        }

        context.base.setString("app_name", appName)
    }
}
