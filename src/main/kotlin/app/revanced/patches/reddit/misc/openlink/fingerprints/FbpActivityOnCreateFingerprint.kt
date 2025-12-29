package app.revanced.patches.reddit.misc.openlink.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

internal object FbpActivityOnCreateFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/FbpActivity;") &&
                methodDef.name == "onCreate"
    }
)