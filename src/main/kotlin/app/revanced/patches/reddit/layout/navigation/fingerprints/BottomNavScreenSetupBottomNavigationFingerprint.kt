package app.revanced.patches.reddit.layout.navigation.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenSetupBottomNavigationFingerprint.indexOfButtonsArrayInstruction
import app.revanced.util.containsWideLiteralInstructionValue
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.TypeReference

internal object BottomNavScreenSetupBottomNavigationFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(Opcode.FILLED_NEW_ARRAY),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(1906671695L) &&
                methodDef.name == "invoke" &&
                indexOfButtonsArrayInstruction(methodDef) >= 0
    }
) {
    fun indexOfButtonsArrayInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.FILLED_NEW_ARRAY &&
                    getReference<TypeReference>()?.type?.startsWith("[Lcom/reddit/widget/bottomnav/") == true
        }
}