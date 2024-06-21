package app.revanced.patches.shared.spoofsignature

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.shared.spoofsignature.fingerprints.CertificateFingerprint
import app.revanced.patches.shared.spoofsignature.fingerprints.CertificateFingerprint.GET_PACKAGE_NAME_METHOD_REFERENCE
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

abstract class BaseSpoofSignaturePatch(
    private val packageName: String
) : BytecodePatch(
    setOf(CertificateFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        CertificateFingerprint.resultOrThrow().mutableClass.methods.forEach { mutableMethod ->
            mutableMethod.apply {
                val getPackageNameIndex =
                    getTargetIndexWithReference(GET_PACKAGE_NAME_METHOD_REFERENCE) + 1

                if (getPackageNameIndex > 0) {
                    val targetRegister = (getInstruction(getPackageNameIndex) as OneRegisterInstruction).registerA

                    replaceInstruction(
                        getPackageNameIndex,
                        "const-string v$targetRegister, \"$packageName\"",
                    )
                }
            }
        }
    }
}
