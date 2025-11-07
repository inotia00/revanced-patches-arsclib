package app.revanced.patches.reddit.ad.comments.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

internal object CommentAdCommentScreenAdViewFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("ad"),
    customFingerprint = { _, classDef ->
        classDef.type.endsWith("/CommentScreenAdView;")
    },
)
