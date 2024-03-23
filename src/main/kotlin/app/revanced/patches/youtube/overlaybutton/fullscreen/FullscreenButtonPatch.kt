package app.revanced.patches.youtube.overlaybutton.fullscreen

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.overlaybutton.fullscreen.fingerprints.FullScreenButtonFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.exception
import app.revanced.util.getTargetIndexWithReference
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch(dependencies = [SharedResourceIdPatch::class])
object FullscreenButtonPatch : BytecodePatch(
    setOf(FullScreenButtonFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        FullScreenButtonFingerprint.result?.let {
            it.mutableMethod.apply {
                val viewIndex = getTargetIndexWithReference("Landroid/widget/ImageView;->getResources()Landroid/content/res/Resources;")
                val viewRegister = getInstruction<FiveRegisterInstruction>(viewIndex).registerC

                addInstructionsWithLabels(
                    viewIndex, """
                        invoke-static {v$viewRegister}, $UTILS_PATH/FullscreenButtonPatch;->hideFullscreenButton(Landroid/widget/ImageView;)Landroid/widget/ImageView;
                        move-result-object v$viewRegister
                        if-nez v$viewRegister, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(viewIndex))
                )
            }
        } ?: throw FullScreenButtonFingerprint.exception

    }
}
