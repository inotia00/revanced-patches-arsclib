package app.revanced.patches.music.utils.sponsorblock

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.ResourceUtils
import app.revanced.patches.music.utils.settings.ResourceUtils.hookPreference
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.patch.BaseResourcePatch

@Suppress("DEPRECATION", "unused")
object SponsorBlockPatch : BaseResourcePatch(
    name = "SponsorBlock",
    description = "Adds options to enable and configure SponsorBlock, which can skip undesired video segments such as non-music sections.",
    dependencies = setOf(
        SettingsPatch::class,
        SponsorBlockBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        /**
         * Copy preference
         */
        arrayOf(
            ResourceGroup(
                "xml",
                "sponsorblock_prefs.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("music/sponsorblock", resourceGroup)
        }

        /**
         * Hook SponsorBlock preference
         */
        context.hookPreference(
            "revanced_sponsorblock_settings",
            "com.google.android.apps.youtube.music.settings.fragment.AdvancedPrefsFragmentCompat"
        )

        val publicFile = context["res/values/public.xml"]
        val preferenceFile = context["res/xml/sponsorblock_prefs.xml"]

        publicFile.writeText(
            publicFile.readText()
                .replace(
                    "\"advanced_prefs_compat\"",
                    "\"sponsorblock_prefs\""
                )
        )

        preferenceFile.writeText(
            preferenceFile.readText()
                .replace(
                    "\"com.google.android.apps.youtube.music\"",
                    "\"" + ResourceUtils.musicPackageName + "\""
                )
        )

    }
}