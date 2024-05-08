package app.revanced.patches.reddit.utils.settings.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

internal object OssLicensesMenuActivityOnCreateFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
        Opcode.INVOKE_STATIC
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/OssLicensesMenuActivity;") &&
                methodDef.name == "onCreate"
    }
)