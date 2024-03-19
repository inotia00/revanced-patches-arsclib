package app.revanced.patches.music.actionbar.component.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

object LikeDislikeContainerVisibilityFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT
    ),
    customFingerprint = custom@{ methodDef, _ ->
        if (methodDef.implementation == null)
            return@custom false

        for (instruction in methodDef.implementation!!.instructions) {
            if (instruction.opcode != Opcode.INVOKE_VIRTUAL)
                continue

            val referenceInstruction = instruction as ReferenceInstruction
            if (referenceInstruction.reference.toString() != "Landroid/view/View;->setVisibility(I)V")
                continue

            return@custom true
        }
        return@custom false
    }
)