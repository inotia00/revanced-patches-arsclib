package app.revanced.patches.music.misc.codecs

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object OpusCodecPatch : BaseResourcePatch(
    name = "Enable OPUS codec",
    description = "Adds an options to enable the OPUS audio codec if the player response includes.",
    dependencies = setOf(
        OpusCodecBytecodePatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        SettingsPatch.addSwitchPreference(
            CategoryType.MISC,
            "revanced_enable_opus_codec",
            "false"
        )

    }
}
