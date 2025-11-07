package app.revanced.patches.reddit.ad.comments.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

internal object CommentsViewModelConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, classDef ->
        classDef.superclass == "Lcom/reddit/screen/presentation/CompositionViewModel;" &&
                methodDef.definingClass.endsWith("/CommentsViewModel;") &&
                methodDef.name == "<init>"
    },
)
