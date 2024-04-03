package app.revanced.patches.youtube.misc.updatescreen.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object AppBlockingCheckResultToStringFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("AppBlockingCheckResult{intent=")
)
