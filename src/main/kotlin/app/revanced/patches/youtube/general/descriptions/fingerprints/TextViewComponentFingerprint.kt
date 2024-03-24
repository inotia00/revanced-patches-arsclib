package app.revanced.patches.youtube.general.descriptions.fingerprints

import app.revanced.util.fingerprint.MethodReferenceNameFingerprint
import com.android.tools.smali.dexlib2.Opcode

/**
 * This fingerprint is compatible with YouTube v18.35.xx~
 * Nonetheless, the patch works in YouTube v19.02.xx~
 */
object TextViewComponentFingerprint : MethodReferenceNameFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.CMPL_FLOAT),
    reference = { "setBreakStrategy" }
)
