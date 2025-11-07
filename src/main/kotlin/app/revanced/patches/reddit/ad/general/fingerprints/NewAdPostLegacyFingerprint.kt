package app.revanced.patches.reddit.ad.general.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.NewAdPostLegacyFingerprint.indexOfAddArrayListInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.MethodReference

internal object NewAdPostLegacyFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(Opcode.INVOKE_VIRTUAL),
    strings = listOf(
        "chain",
        "feedElement"
    ),
    customFingerprint = { methodDef, classDef ->
        classDef.sourceFile == "AdElementConverter.kt" &&
                indexOfAddArrayListInstruction(methodDef) >= 0
    },
) {
    fun indexOfAddArrayListInstruction(methodDef: Method, index: Int = 0) =
        methodDef.indexOfFirstInstruction(index) {
            getReference<MethodReference>()?.toString() == "Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z"
        }
}