package app.revanced.patches.youtube.layout.header

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusHeader
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch
import kotlin.io.path.copyTo

@Suppress("DEPRECATION", "unused")
object PremiumHeadingPatch : BaseResourcePatch(
    name = "Premium heading",
    description = "Show or hide the premium heading.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    private const val DEFAULT_HEADING_RES = "yt_wordmark_header"
    private const val PREMIUM_HEADING_RES = "yt_premium_wordmark_header"

    private val UsePremiumHeading by booleanPatchOption(
        key = "UsePremiumHeading",
        default = true,
        title = "Use premium heading",
        description = "Whether to use the premium heading.",
        required = true
    )

    override fun execute(context: ResourceContext) {
        val resDirectory = context["res"]

        val (original, replacement) = if (UsePremiumHeading == true)
            PREMIUM_HEADING_RES to DEFAULT_HEADING_RES
        else
            DEFAULT_HEADING_RES to PREMIUM_HEADING_RES

        val variants = arrayOf("light", "dark")

        arrayOf(
            "xxxhdpi",
            "xxhdpi",
            "xhdpi",
            "hdpi",
            "mdpi"
        ).mapNotNull { dpi ->
            resDirectory.resolve("drawable-$dpi").takeIf { it.exists() }?.toPath()
        }.also {
            if (it.isEmpty())
                throw PatchException("The drawable folder can not be found. Therefore, the patch can not be applied.")
        }.forEach { path ->

            variants.forEach { mode ->
                val fromPath = path.resolve("${original}_$mode.png")
                val toPath = path.resolve("${replacement}_$mode.png")

                fromPath.copyTo(toPath, true)
            }
        }

        val header = if (UsePremiumHeading == true)
            "Premium"
        else
            "Default"

        context.updatePatchStatusHeader(header)
    }
}
