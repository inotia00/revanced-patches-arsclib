package app.revanced.patches.reddit.ad.general.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

internal object AdPostSectionConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("sections"),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "<init>"
    }
)
