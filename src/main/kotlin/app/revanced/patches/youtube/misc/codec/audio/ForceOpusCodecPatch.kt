package app.revanced.patches.youtube.misc.codec.audio

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object ForceOpusCodecPatch : BaseResourcePatch(
    name = "Force opus codec",
    description = "Adds an option to force the opus audio codec instead of the mp4a audio codec.",
    dependencies = setOf(
        ForceOpusCodecBytecodePatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: EXPERIMENTAL_FLAGS",
                "SETTINGS: ENABLE_OPUS_CODEC"
            )
        )

        SettingsPatch.updatePatchStatus("Force opus codec")

    }
}
