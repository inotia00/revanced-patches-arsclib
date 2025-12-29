package app.revanced.patches.reddit.misc.openlink.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.misc.openlink.fingerprints.ArticleConstructorFingerprint.indexOfNullCheckInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.MethodReference

internal object ArticleConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    strings = listOf("url"),
    customFingerprint = { methodDef, _ ->
        indexOfNullCheckInstruction(methodDef) >= 0
    }
) {
    fun indexOfNullCheckInstruction(methodDef: Method, startIndex: Int = 0) =
        methodDef.indexOfFirstInstruction(startIndex) {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_STATIC &&
                    reference?.returnType == "V" &&
                    reference.parameterTypes.size == 2 &&
                    reference.parameterTypes[1] == "Ljava/lang/String;"
        }
}