package app.revanced.patches.music.misc.tastebuilder

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.misc.tastebuilder.fingerprints.TasteBuilderConstructorFingerprint
import app.revanced.patches.music.misc.tastebuilder.fingerprints.TasteBuilderSyntheticFingerprint
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MusicTasteBuilderShelf
import app.revanced.util.getTargetIndex
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object TasteBuilderPatch : BaseBytecodePatch(
    name = "Hide taste builder",
    description = "Hides the \"Tell us which artists you like\" card from the homepage.",
    dependencies = setOf(SharedResourceIdPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(TasteBuilderConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        TasteBuilderConstructorFingerprint.resultOrThrow().let { parentResult ->
            TasteBuilderSyntheticFingerprint.resolve(context, parentResult.classDef)

            parentResult.mutableMethod.apply {
                val freeRegister = implementation!!.registerCount - parameters.size - 2
                val constIndex = getWideLiteralInstructionIndex(MusicTasteBuilderShelf)
                val targetIndex = getTargetIndex(constIndex, Opcode.MOVE_RESULT_OBJECT)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        const/16 v$freeRegister, 0x8
                        invoke-virtual {v$targetRegister, v$freeRegister}, Landroid/view/View;->setVisibility(I)V
                        """
                )
            }
        }

        TasteBuilderSyntheticFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$insertRegister, 0x0"
                )
            }
        }
    }
}
