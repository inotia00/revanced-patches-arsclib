package app.revanced.patches.reddit.ad.general.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.ImmutableListBuilderFingerprint.indexOfAutoplayVideoPreviewsOptionInstruction
import app.revanced.patches.reddit.ad.general.fingerprints.ImmutableListBuilderFingerprint.indexOfImmutableListBuilderInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.MethodReference

internal object ImmutableListBuilderFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = emptyList(),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "<clinit>" &&
                indexOfAutoplayVideoPreviewsOptionInstruction(methodDef) >= 0 &&
                indexOfImmutableListBuilderInstruction(methodDef) >= 0
    }
) {
    fun indexOfAutoplayVideoPreviewsOptionInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_STATIC &&
                    reference?.name == "getEntries" &&
                    reference.definingClass == "Lcom/reddit/accessibility/AutoplayVideoPreviewsOption;"
        }

    fun indexOfImmutableListBuilderInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_STATIC &&
                    reference?.parameterTypes?.size == 1 &&
                    reference.parameterTypes.firstOrNull() == "Ljava/lang/Iterable;"
        }
}
