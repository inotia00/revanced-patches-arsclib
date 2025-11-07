package app.revanced.patches.reddit.utils.settings.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

internal object WebBrowserActivityOnCreateFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("com.reddit.extra.initial_url"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/WebBrowserActivity;") &&
                methodDef.name == "onCreate"
    }
)