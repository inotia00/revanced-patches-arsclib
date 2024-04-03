package app.revanced.patches.youtube.utils.playercontrols.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object UserScrubbingFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("Z"),
    opcodes = listOf(Opcode.OR_INT_LIT8),
    literalSupplier = { 64 }
)