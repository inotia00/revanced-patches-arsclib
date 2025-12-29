package app.revanced.patches.reddit.misc.openlink.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

internal object ArticleToStringFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("Article(postId="),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "toString"
    }
)