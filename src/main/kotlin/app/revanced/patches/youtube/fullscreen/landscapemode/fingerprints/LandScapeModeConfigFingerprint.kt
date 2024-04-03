package app.revanced.patches.youtube.fullscreen.landscapemode.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

/**
 * This fingerprint is compatible with YouTube v18.42.41+
 */
internal object LandScapeModeConfigFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    literalSupplier = { 45446428 }
)