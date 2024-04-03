package app.revanced.patches.youtube.utils.videoid.general.fingerprint

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object VideoIdParentFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Exception;"),
    strings = listOf("error retrieving subtitle"),
)
