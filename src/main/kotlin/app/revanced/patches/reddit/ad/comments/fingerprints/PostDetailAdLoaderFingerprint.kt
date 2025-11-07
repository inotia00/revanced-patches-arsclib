package app.revanced.patches.reddit.ad.comments.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

internal object PostDetailAdLoaderFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.contains("/RedditPostDetailAdLoader\$loadPostDetailAds\$")
                && methodDef.name == "invokeSuspend"
    },
)
