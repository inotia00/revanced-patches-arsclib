package app.revanced.patches.music.general.historybutton

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.general.historybutton.fingerprints.HistoryMenuItemFingerprint
import app.revanced.patches.music.general.historybutton.fingerprints.HistoryMenuItemOfflineTabFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
object HistoryButtonPatch : BaseBytecodePatch(
    name = "Hide history button",
    description = "Adds an option to hide the history button in the toolbar.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        HistoryMenuItemFingerprint,
        HistoryMenuItemOfflineTabFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        arrayOf(
            HistoryMenuItemFingerprint,
            HistoryMenuItemOfflineTabFingerprint
        ).forEach { fingerprint ->
            fingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.startIndex
                    val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->hideHistoryButton(Z)Z
                            move-result v$insertRegister
                            """
                    )
                }
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_history_button",
            "false"
        )

    }
}
