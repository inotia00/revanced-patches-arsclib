package app.revanced.patches.music.flyoutpanel.compactdialog

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patches.music.flyoutpanel.compactdialog.fingerprints.DialogSolidFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.FLYOUT_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getWalkerMethod
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object CompactDialogPatch : BaseBytecodePatch(
    name = "Enable compact dialog",
    description = "Adds an option to enable the compact flyout menu on phones.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(DialogSolidFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        DialogSolidFingerprint.resultOrThrow().let {
            val walkerMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.endIndex)
            walkerMethod.addInstructions(
                2, """
                    invoke-static {p0}, $FLYOUT_CLASS_DESCRIPTOR->enableCompactDialog(I)I
                    move-result p0
                    """
            )
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_enable_compact_dialog",
            "true"
        )

    }
}
