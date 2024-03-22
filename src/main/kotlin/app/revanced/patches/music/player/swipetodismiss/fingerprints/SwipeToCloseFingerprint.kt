package app.revanced.patches.music.player.swipetodismiss.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

object SwipeToCloseFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    literalSupplier = { 45398432 }
)