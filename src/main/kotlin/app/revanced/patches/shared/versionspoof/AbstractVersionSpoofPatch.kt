package app.revanced.patches.shared.versionspoof

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.shared.versionspoof.fingerprints.ClientInfoFingerprint
import app.revanced.patches.shared.versionspoof.fingerprints.ClientInfoParentFingerprint
import app.revanced.util.exception
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getTargetIndexWithFieldReferenceName
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

abstract class AbstractVersionSpoofPatch(
    private val descriptor: String
) : BytecodePatch(
    setOf(ClientInfoParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        ClientInfoParentFingerprint.result?.let { parentResult ->
            ClientInfoFingerprint.resolve(context, parentResult.classDef)

            ClientInfoFingerprint.result?.let {
                it.mutableMethod.apply {
                    val versionIndex = getTargetIndexWithFieldReferenceName("RELEASE") + 1
                    val insertIndex = getTargetIndexReversed(versionIndex, Opcode.IPUT_OBJECT)
                    val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $descriptor
                            move-result-object v$insertRegister
                            """
                    )
                }
            } ?: throw ClientInfoFingerprint.exception
        } ?: throw ClientInfoParentFingerprint.exception

    }
}