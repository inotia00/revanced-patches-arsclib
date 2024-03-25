package app.revanced.patches.youtube.misc.updatescreen

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.misc.updatescreen.fingerprints.AppBlockingCheckResultToStringFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.util.MethodUtil

@Patch(
    name = "Disable update screen",
    description = "Disable the \"Update your app\" screen that appears when using an outdated client.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39"
            ]
        )
    ]
)
@Suppress("unused")
object UpdateScreenPatch : BytecodePatch(
    setOf(AppBlockingCheckResultToStringFingerprint)
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/UpdateScreenPatch;"

    override fun execute(context: BytecodeContext) {
        AppBlockingCheckResultToStringFingerprint.result?.mutableClass?.methods?.first { method ->
            MethodUtil.isConstructor(method)
                    && method.parameters == listOf("Landroid/content/Intent;", "Z")
        }?.addInstructions(
            1,
            "const/4 p1, 0x0"
        ) ?: throw AppBlockingCheckResultToStringFingerprint.exception

        SettingsPatch.updatePatchStatus("Disable update screen")
    }
}