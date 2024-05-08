package app.revanced.patches.reddit.utils.settings

import app.revanced.patcher.ResourceContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.apk.Apk
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patches.reddit.utils.annotation.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.IntegrationsPatch
import app.revanced.util.ResourceUtils.editText
import app.revanced.util.ResourceUtils.getResourcePath
import kotlin.io.path.exists

@Patch
@Name("Settings")
@Description("Adds ReVanced Extended settings to Reddit.")
@DependsOn([IntegrationsPatch::class, SettingsBytecodePatch::class])
@RedditCompatibility
@RequiresIntegrations
class SettingsPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {
        /**
         * Replace settings icon and label
         */
        arrayOf("preferences", "preferences_logged_in").forEach { targetXML ->
            fun Apk.transform() {
                resources.openFile("res/xml/$targetXML.xml").editText {
                    it.replace(
                        "\"@drawable/icon_text_post\" android:title=\"@string/label_acknowledgements\"",
                        "\"@drawable/icon_beta_planet\" android:title=\"ReVanced Extended\""
                    )
                }
            }
            context.apkBundle.all.forEach(Apk::transform)
        }
    }
}
