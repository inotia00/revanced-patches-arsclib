package app.revanced.patches.reddit.layout.trendingtoday.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

internal object SearchTypeaheadListDefaultPresentationConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "<init>"
    }
)