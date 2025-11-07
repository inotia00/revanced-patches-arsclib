package app.revanced.patches.reddit.layout.subredditdialog.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.NSFWAlertEmitFingerprint.indexOfGetOver18Instruction
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.NSFWAlertEmitFingerprint.indexOfHasBeenVisitedInstruction
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.NSFWAlertEmitFingerprint.indexOfIsIncognitoInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.MethodReference

internal object NSFWAlertEmitFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/Object;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;", "L"),
    strings = listOf("nsfwAlertDelegate"),
    customFingerprint = { methodDef, classDef ->
        classDef.type.startsWith("Lcom/reddit/screens/pager/v2/") &&
                methodDef.name == "emit" &&
                indexOfGetOver18Instruction(methodDef) >= 0 &&
                indexOfHasBeenVisitedInstruction(methodDef) >= 0 &&
                indexOfIsIncognitoInstruction(methodDef) >= 0
    }
) {
    fun indexOfGetOver18Instruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_VIRTUAL &&
                    reference?.name == "getOver18" &&
                    reference.returnType == "Ljava/lang/Boolean;"
        }

    fun indexOfIsIncognitoInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_INTERFACE &&
                    reference?.name == "isIncognito" &&
                    reference.returnType == "Z"
        }

    fun indexOfHasBeenVisitedInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_VIRTUAL &&
                    reference?.name == "getHasBeenVisited" &&
                    reference.returnType == "Z"
        }
}