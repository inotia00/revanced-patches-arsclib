package app.revanced.patches.youtube.layout.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.shared.translations.APP_LANGUAGES
import app.revanced.patches.shared.translations.TranslationsUtils.invoke
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object TranslationsPatch : BaseResourcePatch(
    name = "Translations YouTube",
    description = "Add translations or remove string resources.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    // Array of supported translations, each represented by its language code.
    private val TRANSLATIONS = arrayOf(
        "ar", "el-rGR", "es-rES", "fr-rFR", "hu-rHU", "it-rIT", "ja-rJP", "ko-rKR", "pl-rPL",
        "pt-rBR", "ru-rRU", "tr-rTR", "uk-rUA", "vi-rVN", "zh-rCN", "zh-rTW"
    )

    private var CustomTranslations by stringPatchOption(
        key = "CustomTranslations",
        default = "",
        title = "Custom translations",
        description = """
            The path to the 'strings.xml' file.
            Please note that applying the 'strings.xml' file will overwrite all existing translations.
            """.trimIndent()
    )

    private var SelectedTranslations by stringPatchOption(
        key = "SelectedTranslations",
        default = TRANSLATIONS.joinToString(", "),
        title = "Translations to add",
        description = "A list of translations to be added for the RVX settings, separated by commas."
    )

    private var SelectedStringResources by stringPatchOption(
        key = "SelectedStringResources",
        default = APP_LANGUAGES.joinToString(", "),
        title = "String resources to keep",
        description = """
            A list of string resources to be kept, separated by commas.
            String resources not in the list will be removed from the app.

            Default string resource, English, is not removed.
            """.trimIndent()
    )

    override fun execute(context: ResourceContext) {
        context.invoke(
            CustomTranslations, SelectedTranslations, SelectedStringResources,
            TRANSLATIONS, "youtube"
        )

        SettingsPatch.updatePatchStatus("Translations")
    }
}
