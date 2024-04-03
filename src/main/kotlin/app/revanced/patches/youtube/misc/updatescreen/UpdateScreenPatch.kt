package app.revanced.patches.youtube.misc.updatescreen

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patches.youtube.misc.updatescreen.fingerprints.AppBlockingCheckResultToStringFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
object UpdateScreenPatch : BaseBytecodePatch(
    name = "Disable update screen",
    description = "Disable the \"Update your app\" screen that appears when using an outdated client.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(AppBlockingCheckResultToStringFingerprint)
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/UpdateScreenPatch;"

    override fun execute(context: BytecodeContext) {
        AppBlockingCheckResultToStringFingerprint.resultOrThrow().mutableClass.methods.first { method ->
            MethodUtil.isConstructor(method)
                    && method.parameters == listOf("Landroid/content/Intent;", "Z")
        }.addInstructions(
            1,
            "const/4 p1, 0x0"
        )

        SettingsPatch.updatePatchStatus("Disable update screen")
    }
}