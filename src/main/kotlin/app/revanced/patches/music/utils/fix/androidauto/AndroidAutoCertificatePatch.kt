package app.revanced.patches.music.utils.fix.androidauto

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patches.music.utils.fix.androidauto.fingerprints.CertificateCheckFingerprint
import app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.exception
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object AndroidAutoCertificatePatch : BaseBytecodePatch(
    name = "Certificate spoof",
    description = "Enables YouTube Music to work with Android Auto by spoofing the YouTube Music certificate.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(CertificateCheckFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        CertificateCheckFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        const/4 v0, 0x1
                        return v0
                        """
                )
            }
        } ?: throw CertificateCheckFingerprint.exception

    }
}
