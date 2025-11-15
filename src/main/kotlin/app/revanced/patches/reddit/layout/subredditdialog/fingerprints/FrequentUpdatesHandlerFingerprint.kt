package app.revanced.patches.reddit.layout.subredditdialog.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.layout.subredditdialog.fingerprints.FrequentUpdatesHandlerFingerprint.listOfUserIsSubscriberInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.MethodReference

internal object FrequentUpdatesHandlerFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/Object;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(Opcode.CONST_STRING),
    customFingerprint = { methodDef, classDef ->
        classDef.type.startsWith("Lcom/reddit/screens/pager/FrequentUpdatesHandler${'$'}handleFrequentUpdates${'$'}") &&
                methodDef.name == "invokeSuspend" &&
                listOfUserIsSubscriberInstruction(methodDef).isNotEmpty()
    }
) {
    fun listOfUserIsSubscriberInstruction(methodDef: Method) =
        methodDef.implementation?.instructions
            ?.withIndex()
            ?.filter { (_, instruction) ->
                val reference = (instruction as? ReferenceInstruction)?.reference
                instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                        reference is MethodReference &&
                        reference.name == "getUserIsSubscriber" &&
                        reference.returnType == "Ljava/lang/Boolean;"
            }
            ?.map { (index, _) -> index }
            ?.reversed()
            ?: emptyList()
}