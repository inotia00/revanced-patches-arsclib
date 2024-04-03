package app.revanced.patches.shared.elements

import app.revanced.patcher.data.ResourceContext

@Suppress("DEPRECATION")
object StringsElementsUtils {
    internal fun ResourceContext.removeStringsElements(
        paths: Array<String>,
        replacements: Array<String>
    ) {
        paths.forEach { path ->
            val targetXmlPath = this["res"].resolve(path).resolve("strings.xml")

            if (targetXmlPath.exists()) {
                val targetXml = this["res/$path/strings.xml"]

                replacements.forEach replacementsLoop@{ replacement ->
                    targetXml.writeText(
                        targetXml.readText()
                            .replaceFirst(""" {4}<string name="$replacement".+""".toRegex(), "")
                    )
                }
            }
        }
    }
}

