package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object ThumbnailPreviewConfigFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    literalSupplier = { 45398577 }
)