package app.revanced.patches.music.general.redirection.fingerprints

import app.revanced.util.containsWideLiteralInstructionIndex
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object DislikeButtonOnClickListenerFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE
    ),
    customFingerprint = handler@{ methodDef, _ ->
        if (!methodDef.containsWideLiteralInstructionIndex(53465))
            return@handler false

        methodDef.name == "onClick"
    }
)