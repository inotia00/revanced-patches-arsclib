package app.revanced.patches.youtube.overlaybutton.alwaysrepeat

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.youtube.overlaybutton.alwaysrepeat.fingerprints.AutoNavInformerFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.util.exception
import app.revanced.util.getWalkerMethod
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

object AlwaysRepeatPatch : BytecodePatch(
    setOf(AutoNavInformerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        AutoNavInformerFingerprint.result?.let {
            val walkerMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex)
            walkerMethod.apply {
                val index = implementation!!.instructions.size - 2
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1, """
                        invoke-static {v$register}, $UTILS_PATH/AlwaysRepeatPatch;->enableAlwaysRepeat(Z)Z
                        move-result v$register
                        """
                )
            }
        } ?: throw AutoNavInformerFingerprint.exception

    }
}
