package app.revanced.patches.youtube.layout.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.shared.translations.TranslationsUtils.copyXml
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object TranslationsPatch : BaseResourcePatch(
    name = "Translations",
    description = "Adds Crowdin translations for YouTube.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        context.copyXml(
            "youtube",
            arrayOf(
                "ar",
                "el-rGR",
                "es-rES",
                "fr-rFR",
                "hu-rHU",
                "it-rIT",
                "ja-rJP",
                "ko-rKR",
                "pl-rPL",
                "pt-rBR",
                "ru-rRU",
                "tr-rTR",
                "uk-rUA",
                "vi-rVN",
                "zh-rCN",
                "zh-rTW"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
