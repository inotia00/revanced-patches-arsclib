package app.revanced.meta

import app.revanced.patcher.Context
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.extensions.PatchExtensions.description
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.Patch
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths

internal class ReadmeGenerator : PatchesFileGenerator {
    private companion object {
        private const val TABLE_HEADER =
            "| \uD83D\uDC8A Patch | \uD83D\uDCDC Description | \uD83C\uDFF9 Target Version |\n" +
                    "|:--------:|:--------------:|:-----------------:|"
    }

    override fun generate(bundle: PatchBundlePatches) {
        val readMeFile = File("README.md")
        val readMeTemplateFile = File("README-template.md")

        val output = StringBuilder()

        if (readMeFile.exists()) {
            PrintWriter(readMeFile).also {
                it.print("")
                it.close()
            }
        } else {
            Files.createFile(Paths.get(readMeFile.absolutePath))
        }

        val hashMap = HashMap<String, String>()

        // copy the contents of 'README-template.md' to the temp file
        StringBuilder(readMeTemplateFile.readText())
            .toString()
            .let(readMeFile::writeText)

        mapOf(
            "com.reddit.frontpage" to "\"COMPATIBLE_PACKAGE_REDDIT\"",
        ).forEach { (compatiblePkg, replaceString) ->
            var updated = false

            mutableMapOf<String, MutableList<Class<out Patch<Context>>>>()
                .apply {
                    for (patch in bundle) {
                        patch.compatiblePackages?.forEach { pkg ->
                            if (!contains(pkg.name)) put(pkg.name, mutableListOf())
                            this[pkg.name]!!.add(patch)
                        }
                    }
                }
                .entries
                .sortedByDescending { it.value.size }
                .forEach { (pkg, patches) ->
                    output.apply {
                        appendLine("### [\uD83D\uDCE6 `$pkg`](https://play.google.com/store/apps/details?id=$pkg)")
                        appendLine("<details>\n")
                        appendLine(TABLE_HEADER)
                        patches.sortedBy { it.name }.forEach { patch ->
                            val supportedVersionArray =
                                patch.compatiblePackages?.single { it.name == pkg }?.versions

                            val supportedVersion =
                                if (supportedVersionArray?.isNotEmpty() == true) {
                                    val minVersion = supportedVersionArray.elementAt(0)
                                    val maxVersion =
                                        supportedVersionArray.elementAt(supportedVersionArray.size - 1)
                                    if (minVersion == maxVersion)
                                        maxVersion
                                    else
                                        "$minVersion ~ $maxVersion"
                                } else
                                    "ALL"

                            appendLine(
                                "| `${patch.patchName}` " +
                                        "| ${patch.description} " +
                                        "| $supportedVersion |"
                            )

                            if (!updated && compatiblePkg == pkg) {
                                if (supportedVersionArray?.isNotEmpty() == true && supportedVersion != "ALL") {
                                    val sb = StringBuilder()
                                    sb.appendLine("[")

                                    val i = supportedVersionArray.iterator()

                                    while (i.hasNext()) {
                                        sb.append("        \"${i.next()}")
                                        if (i.hasNext()) {
                                            sb.appendLine("\",")
                                        } else {
                                            sb.appendLine("\"")
                                        }
                                    }
                                    sb.append("      ]")

                                    hashMap[replaceString] = sb.toString()
                                }

                                updated = true
                            }
                        }

                        appendLine("</details>\n")
                    }
                }
        }

        StringBuilder(readMeTemplateFile.readText())
            .replace(Regex("\\{\\{\\s?table\\s?}}"), output.toString())
            .let(readMeFile::writeText)

        hashMap.forEach { (k, v) ->
            StringBuilder(readMeFile.readText())
                .replace(Regex(k), v)
                .let(readMeFile::writeText)
        }
    }
}