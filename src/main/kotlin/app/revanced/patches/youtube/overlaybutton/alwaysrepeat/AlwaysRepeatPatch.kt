package app.revanced.patches.youtube.overlaybutton.alwaysrepeat

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.youtube.overlaybutton.alwaysrepeat.fingerprints.AutoNavInformerFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getWalkerMethod
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

object AlwaysRepeatPatch : BytecodePatch(
    setOf(AutoNavInformerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        AutoNavInformerFingerprint.resultOrThrow().let {
            val walkerMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex)
            walkerMethod.apply {
                val index = getTargetIndexReversed(Opcode.IGET_BOOLEAN)
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1, """
                        invoke-static {v$register}, $UTILS_PATH/AlwaysRepeatPatch;->enableAlwaysRepeat(Z)Z
                        move-result v$register
                        """
                )
            }
        }

    }
}
