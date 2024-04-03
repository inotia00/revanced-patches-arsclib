package app.revanced.patches.music.flyoutpanel.component.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object SleepTimerFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    literalSupplier = { 45372767 }
)