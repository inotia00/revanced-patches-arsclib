package app.revanced.patches.youtube.utils.fix.client

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.shared.spoofsignature.BaseSpoofSignaturePatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch

object SpoofSignaturePatch : BaseSpoofSignaturePatch(
    packageName = "com.google.android.youtube",
    dependencies = setOf(SettingsPatch::class),
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_CATEGORY: MISC_EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_SIGNATURE"
            )
        )
    }
}