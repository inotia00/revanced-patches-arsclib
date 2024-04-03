package app.revanced.patches.youtube.misc.debugging

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object DebuggingPatch : BaseResourcePatch(
    name = "Enable debug logging",
    description = "Adds an option to enable debug logging.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_DEBUG_LOGGING"
            )
        )

        SettingsPatch.updatePatchStatus("Enable debug logging")


    }
}
