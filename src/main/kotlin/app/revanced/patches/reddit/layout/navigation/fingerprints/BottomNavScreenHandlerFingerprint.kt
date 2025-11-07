package app.revanced.patches.reddit.layout.navigation.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenHandlerFingerprint.indexOfGetItemsInstruction
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenHandlerFingerprint.indexOfSetSelectedItemTypeInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.MethodReference

internal object BottomNavScreenHandlerFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L", "Z", "Landroid/view/ViewGroup;", "L"),
    customFingerprint = { methodDef, _ ->
        indexOfGetItemsInstruction(methodDef) >= 0 &&
                indexOfSetSelectedItemTypeInstruction(methodDef) >= 0
    }
) {
    fun indexOfGetItemsInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()?.toString()
            reference != null && reference.endsWith("getItems()Ljava/util/List;")
        }
    fun indexOfSetSelectedItemTypeInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "setSelectedItemType"
        }
}