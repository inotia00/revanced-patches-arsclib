package app.revanced.patches.shared.opus

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.opus.fingerprints.CodecReferenceFingerprint
import app.revanced.patches.shared.opus.fingerprints.CodecSelectorFingerprint
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference

abstract class BaseOpusCodecsPatch(
    private val descriptor: String
) : BytecodePatch(
    setOf(
        CodecReferenceFingerprint,
        CodecSelectorFingerprint
    )
) {
    private lateinit var targetReference: Reference

    override fun execute(context: BytecodeContext) {

        CodecReferenceFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithReference("Ljava/util/Set;")
                targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference
            }
        }

        CodecSelectorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructionsWithLabels(
                    targetIndex + 1, """
                        invoke-static {}, $descriptor
                        move-result v7
                        if-eqz v7, :mp4a
                        invoke-static {}, $targetReference
                        move-result-object v$targetRegister
                        """, ExternalLabel("mp4a", getInstruction(targetIndex + 1))
                )
            }
        }
    }
}
