package app.revanced.patches.shared.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.util.classLoader
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Suppress("DEPRECATION")
object TranslationsUtils {
    internal fun ResourceContext.copyXml(
        sourceDirectory: String,
        languageArray: Array<String>
    ) {
        languageArray.forEach { language ->
            val directory = "values-$language-v21"
            val relativePath = "$language/strings.xml"

            this["res/$directory"].mkdir()

            Files.copy(
                classLoader.getResourceAsStream("$sourceDirectory/translations/$relativePath")!!,
                this["res"].resolve("$directory/strings.xml").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}
