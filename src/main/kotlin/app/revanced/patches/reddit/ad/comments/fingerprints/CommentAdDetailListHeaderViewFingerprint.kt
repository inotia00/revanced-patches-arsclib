package app.revanced.patches.reddit.ad.comments.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.reference.TypeReference

internal object CommentAdDetailListHeaderViewFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Z", "L", "I"),
    customFingerprint = { methodDef, classDef ->
        classDef.superclass == "Lcom/reddit/screen/presentation/CompositionViewModel;" &&
                methodDef.indexOfFirstInstruction {
                    opcode == Opcode.NEW_INSTANCE &&
                            getReference<TypeReference>()?.type?.startsWith("Lcom/reddit/postdetail/comment/refactor/CommentsViewModel\$LoadAdsSeparately\$") == true
                } >= 0
    },
)
