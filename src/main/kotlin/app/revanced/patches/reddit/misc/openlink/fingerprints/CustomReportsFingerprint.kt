package app.revanced.patches.reddit.misc.openlink.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.misc.openlink.fingerprints.CustomReportsFingerprint.indexOfScreenNavigator
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.ReferenceInstruction

internal object CustomReportsFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("https://www.crisistextline.org/"),
    customFingerprint = { methodDef, classDef ->
        classDef.type.contains("/customreports/") &&
                indexOfScreenNavigator(methodDef) >= 0
    }
) {
    fun indexOfScreenNavigator(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            (this as? ReferenceInstruction)?.reference?.toString()
                ?.contains("Landroid/app/Activity;Landroid/net/Uri;") == true
        }
}