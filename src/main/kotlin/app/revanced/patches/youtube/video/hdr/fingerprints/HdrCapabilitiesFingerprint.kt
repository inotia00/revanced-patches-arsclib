package app.revanced.patches.youtube.video.hdr.fingerprints

import app.revanced.util.fingerprint.MethodReferenceNameFingerprint

object HdrCapabilitiesFingerprint : MethodReferenceNameFingerprint(
    returnType = "Z",
    parameters = listOf("I", "Landroid/view/Display;"),
    reference = { "getSupportedHdrTypes" }
)
