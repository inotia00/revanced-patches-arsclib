package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.shorts.components.fingerprints.BottomNavigationBarFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.RenderBottomNavigationBarFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.SetPivotBarFingerprint
import app.revanced.patches.youtube.utils.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS
import app.revanced.util.exception
import app.revanced.util.getTargetIndexWithMethodReferenceName
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

object ShortsNavigationBarPatch : BytecodePatch(
    setOf(
        BottomNavigationBarFingerprint,
        PivotBarCreateButtonViewFingerprint,
        RenderBottomNavigationBarFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        PivotBarCreateButtonViewFingerprint.result?.let { parentResult ->
            SetPivotBarFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val register = getInstruction<OneRegisterInstruction>(startIndex).registerA

                    addInstruction(
                        startIndex + 1,
                        "sput-object v$register, $SHORTS->pivotBar:Ljava/lang/Object;"
                    )
                }
            } ?: throw SetPivotBarFingerprint.exception
        } ?: throw PivotBarCreateButtonViewFingerprint.exception

        RenderBottomNavigationBarFingerprint.result?.let {
            val targetMethod = context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.startIndex + 1, true)
                .getMethod() as MutableMethod

            targetMethod.addInstruction(
                0,
                "invoke-static {}, $SHORTS->hideShortsPlayerNavigationBar()V"
            )
        } ?: throw RenderBottomNavigationBarFingerprint.exception

        BottomNavigationBarFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithMethodReferenceName("findViewById") + 1
                val insertRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerNavigationBar(Landroid/view/View;)Landroid/view/View;
                        move-result-object v$insertRegister
                        """
                )
            }
        } ?: throw BottomNavigationBarFingerprint.exception

    }
}