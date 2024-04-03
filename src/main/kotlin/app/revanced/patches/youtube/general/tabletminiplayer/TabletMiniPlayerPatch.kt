package app.revanced.patches.youtube.general.tabletminiplayer

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerDimensionsCalculatorFingerprint
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerOverrideFingerprint
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerOverrideNoContextFingerprint
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerResponseModelSizeCheckFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getWalkerMethod
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object TabletMiniPlayerPatch : BaseBytecodePatch(
    name = "Enable tablet mini player",
    description = "Adds an option to enable the tablet mini player layout.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        MiniPlayerDimensionsCalculatorFingerprint,
        MiniPlayerResponseModelSizeCheckFingerprint,
        MiniPlayerOverrideFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        MiniPlayerDimensionsCalculatorFingerprint.result?.let { parentResult ->
            MiniPlayerOverrideNoContextFingerprint.resolve(context, parentResult.classDef)
            MiniPlayerOverrideNoContextFingerprint.result?.let {
                it.mutableMethod.apply {
                    hook(getTargetIndex(Opcode.RETURN))
                    hook(getTargetIndexReversed(Opcode.RETURN))
                }
            } ?: throw MiniPlayerOverrideNoContextFingerprint.exception
        } ?: throw MiniPlayerDimensionsCalculatorFingerprint.exception

        MiniPlayerOverrideFingerprint.result?.let {
            it.mutableMethod.apply {
                val walkerMethod = getWalkerMethod(context, getStringInstructionIndex("appName") + 2)

                walkerMethod.apply {
                    hook(getTargetIndex(Opcode.RETURN))
                    hook(getTargetIndexReversed(Opcode.RETURN))
                }
            }
        } ?: throw MiniPlayerOverrideFingerprint.exception

        MiniPlayerResponseModelSizeCheckFingerprint.result?.let {
            it.mutableMethod.apply {
                hook(it.scanResult.patternScanResult!!.endIndex)
            }
        } ?: throw MiniPlayerResponseModelSizeCheckFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: ENABLE_TABLET_MINI_PLAYER"
            )
        )

        SettingsPatch.updatePatchStatus("Enable tablet mini player")

    }

    private fun MutableMethod.hook(index: Int) {
        val register = getInstruction<OneRegisterInstruction>(index).registerA

        addInstructions(
            index, """
                invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->enableTabletMiniPlayer(Z)Z
                move-result v$register
                """
        )
    }
}
