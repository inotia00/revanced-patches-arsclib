package app.revanced.patches.youtube.general.trendingsearches

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.youtube.general.trendingsearches.fingerprints.TrendingSearchConfigFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object TrendingSearchesPatch : BaseBytecodePatch(
    name = "Hide trending searches",
    description = "Adds an option to hide trending searches in the search bar.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(TrendingSearchConfigFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        TrendingSearchConfigFingerprint.literalInstructionBooleanHook(
            45399984,
            "$GENERAL_CLASS_DESCRIPTOR->hideTrendingSearches(Z)Z"
        )

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_TRENDING_SEARCHES"
            )
        )

        SettingsPatch.updatePatchStatus("Hide trending searches")

    }
}