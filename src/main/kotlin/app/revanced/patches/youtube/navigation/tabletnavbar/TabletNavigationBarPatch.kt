package app.revanced.patches.youtube.navigation.tabletnavbar

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patches.youtube.navigation.tabletnavbar.fingerprints.PivotBarChangedFingerprint
import app.revanced.patches.youtube.navigation.tabletnavbar.fingerprints.PivotBarStyleFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.NAVIGATION_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object TabletNavigationBarPatch : BaseBytecodePatch(
    name = "Enable tablet navigation bar",
    description = "Adds an option to enable the tablet navigation bar.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        PivotBarChangedFingerprint,
        PivotBarStyleFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        arrayOf(
            PivotBarChangedFingerprint,
            PivotBarStyleFingerprint
        ).forEach {
            it.resultOrThrow().insertHook()
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: ENABLE_TABLET_NAVIGATION_BAR"
            )
        )

        SettingsPatch.updatePatchStatus("Enable tablet navigation bar")

    }

    private fun MethodFingerprintResult.insertHook() {
        val targetIndex = scanResult.patternScanResult!!.startIndex + 1
        val register =
            mutableMethod.getInstruction<OneRegisterInstruction>(targetIndex).registerA

        mutableMethod.addInstructions(
            targetIndex + 1, """
                invoke-static {v$register}, $NAVIGATION_CLASS_DESCRIPTOR->enableTabletNavBar(Z)Z
                move-result v$register
                """
        )
    }
}
