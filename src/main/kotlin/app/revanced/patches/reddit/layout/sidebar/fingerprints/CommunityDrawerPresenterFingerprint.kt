package app.revanced.patches.reddit.layout.sidebar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.layout.sidebar.fingerprints.CommunityDrawerPresenterFingerprint.indexOfKotlinCollectionInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.MethodReference

internal object CommunityDrawerPresenterFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.XOR_INT_2ADDR,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    customFingerprint = { methodDef, _ ->
        indexOfKotlinCollectionInstruction(methodDef) >= 0
    }
) {
    fun indexOfKotlinCollectionInstruction(
        methodDef: Method,
        startIndex: Int = 0
    ) = methodDef.indexOfFirstInstruction(startIndex) {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_STATIC &&
                reference?.returnType == "Ljava/util/ArrayList;" &&
                reference.definingClass.startsWith("Lkotlin/collections/") &&
                reference.parameterTypes.size == 2 &&
                reference.parameterTypes[0].toString() == "Ljava/lang/Iterable;" &&
                reference.parameterTypes[1].toString() == "Ljava/util/Collection;"
    }

}