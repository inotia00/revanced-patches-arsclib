package app.revanced.patches.reddit.misc.versioncode

import app.revanced.patcher.ResourceContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.util.ResourceUtils.manifestEditor
import app.revanced.util.ResourceUtils.valueOrThrow
import org.w3c.dom.Element

@Patch
@Name("Change version code")
@Description("Changes the version code of the app. By default the highest version code is set. " +
        "This allows older versions of an app to be installed " +
        "if their version code is set to the same or a higher value and can stop app stores to update the app.")
@RedditCompatibility
@Suppress("unused")
class ChangeVersionCodePatch : ResourcePatch {
    private fun throwVersionCodeException(versionCodeString: String): PatchException =
        PatchException(
            "Invalid versionCode: $versionCodeString, " +
                    "Version code should be larger than 1 and smaller than ${Int.MAX_VALUE}."
        )

    companion object : OptionsContainer() {
        private var ChangeVersionCode by option(
            PatchOption.BooleanOption(
                key = "ChangeVersionCode",
                default = false,
                title = "Change version code",
                description = "Changes the version code of the app.",
                required = true,
            )
        )

        private var VersionCode = option(
            PatchOption.StringOption(
                key = "VersionCode",
                default = Int.MAX_VALUE.toString(),
                title = "Version code",
                description = "The version code to use.",
                required = true,
            )
        )
    }

    override fun execute(context: ResourceContext) {
        if (ChangeVersionCode == false) {
            println("INFO: Version code will remain unchanged as 'ChangeVersionCode' is false.")
            return
        }

        val versionCodeString = VersionCode.valueOrThrow()
        val versionCode: Int

        try {
            versionCode = Integer.parseInt(versionCodeString)
        } catch (e: NumberFormatException) {
            throw throwVersionCodeException(versionCodeString)
        }

        if (versionCode < 1) {
            throw throwVersionCodeException(versionCodeString)
        }

        context.manifestEditor().use {
            val manifestElement = it
                .file
                .getElementsByTagName("manifest")
                .item(0) as Element

            manifestElement.setAttribute("android:versionCode", "$versionCode")
        }
    }
}
