package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelPivotButton
import app.revanced.util.fingerprint.LiteralValueFingerprint

object ShortsPivotFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { ReelPivotButton }
)