package app.revanced.patches.youtube.ads.fullscreen

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.ads.fullscreen.fingerprints.InterstitialsContainerFingerprint
import app.revanced.patches.youtube.ads.fullscreen.fingerprints.ShowDialogCommandFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.InterstitialsContainer
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(dependencies = [SharedResourceIdPatch::class])
object FullscreenAdsPatch : BytecodePatch(
    setOf(
        InterstitialsContainerFingerprint,
        ShowDialogCommandFingerprint
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/AdsFilter;"

    override fun execute(context: BytecodeContext) {
        /**
         * Hides fullscreen ads
         * Non-litho view, used in some old clients.
         */
        InterstitialsContainerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(InterstitialsContainer) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $FILTER_CLASS_DESCRIPTOR->hideFullscreenAds(Landroid/view/View;)V"
                )
            }
        }

        /**
         * Hides fullscreen ads
         * Litho view, used in 'ShowDialogCommandOuterClass' in innertube
         */
        ShowDialogCommandFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                // In this method, custom dialog is created and shown.
                // There were no issues despite adding “return-void” to the first index.
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $FILTER_CLASS_DESCRIPTOR->hideFullscreenAds()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(0))
                )

                // If an issue occurs due to patching due to server-side changes in the future,
                // Find the instruction whose name is "show" in [MethodReference] and click the 'AlertDialog.BUTTON_POSITIVE' button.
                //
                // In this case, an instruction for 'getButton' must be added to smali, not in integrations
                // (This custom dialog cannot be cast to [AlertDialog] or [Dialog])
                //
                // See the comments below.

                // val dialogIndex = getTargetIndexWithMethodReferenceName("show")
                // val dialogReference = getInstruction<ReferenceInstruction>(dialogIndex).reference
                // val dialogDefiningClass = (dialogReference as MethodReference).definingClass
                // val getButtonMethod = context.findClass(dialogDefiningClass)!!
                //     .mutableClass.methods.first { method ->
                //         method.parameters == listOf("I")
                //                 && method.returnType == "Landroid/widget/Button;"
                //     }
                // val getButtonCall = dialogDefiningClass + "->" + getButtonMethod.name + "(I)Landroid/widget/Button;"
                // val dialogRegister = getInstruction<FiveRegisterInstruction>(dialogIndex).registerC
                // val freeIndex = getTargetIndex(dialogIndex, Opcode.IF_EQZ)
                // val freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

                // addInstructions(
                //     dialogIndex + 1, """
                //         # Get the 'AlertDialog.BUTTON_POSITIVE' from custom dialog
                //         # Since this custom dialog cannot be cast to AlertDialog or Dialog,
                //         # It should come from smali, not integrations.
                //         const/4 v$freeRegister, -0x1
                //         invoke-virtual {v$dialogRegister, $freeRegister}, $getButtonCall
                //         move-result-object $freeRegister
                //         invoke-static {$freeRegister}, $FULLSCREEN_ADS_CLASS_DESCRIPTOR->confirmDialog(Landroid/widget/Button;)V
                //         """
                // )
            }
        }
    }
}
