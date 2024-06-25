package app.revanced.patches.reddit.layout.subredditdialog.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.RedditAlertDialogsFingerprint.indexOfSetBackgroundTintListInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.MethodReference

internal object RedditAlertDialogsFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lcom/reddit/screen/dialog/RedditAlertDialog;"
                && indexOfSetBackgroundTintListInstruction(methodDef) >= 0
    }
) {
    fun indexOfSetBackgroundTintListInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL && getReference<MethodReference>()?.name == "setBackgroundTintList"
        }
}