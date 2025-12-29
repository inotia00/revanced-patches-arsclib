package app.revanced.patches.reddit.utils.settings.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.utils.settings.fingerprints.PreferenceManagerFingerprint.indexOfPreferencesPresenterInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.TypeReference

internal object PreferenceManagerFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ ->
        indexOfPreferencesPresenterInstruction(methodDef) >= 0
    }
) {
    fun indexOfPreferencesPresenterInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.NEW_INSTANCE &&
                    getReference<TypeReference>()?.type?.contains("checkIfShouldShowImpressumOption") == true
        }
}