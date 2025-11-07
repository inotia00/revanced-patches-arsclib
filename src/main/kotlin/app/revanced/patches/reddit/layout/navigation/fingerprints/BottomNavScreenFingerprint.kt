package app.revanced.patches.reddit.layout.navigation.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenFingerprint.indexOfListBuilderInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.MethodReference

internal object BottomNavScreenFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/content/res/Resources;"),
    strings = listOf("answersFeatures"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lcom/reddit/launch/bottomnav/BottomNavScreen;" &&
                indexOfListBuilderInstruction(methodDef) >= 0
    }
) {
    fun indexOfListBuilderInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.toString() == "Lkotlin/collections/builders/ListBuilder;->build()Ljava/util/List;"
        }
}