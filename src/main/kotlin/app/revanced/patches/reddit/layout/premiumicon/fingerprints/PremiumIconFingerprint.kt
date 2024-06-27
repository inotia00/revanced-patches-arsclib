package app.revanced.patches.reddit.layout.premiumicon.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

internal object PremiumIconFingerprint : MethodFingerprint(
    returnType = "Z",
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lcom/reddit/domain/model/MyAccount;"
                && methodDef.name == "isPremiumSubscriber"
    }
)