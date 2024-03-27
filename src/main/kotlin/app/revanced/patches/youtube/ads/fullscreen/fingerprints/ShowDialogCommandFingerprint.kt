package app.revanced.patches.youtube.ads.fullscreen.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.SlidingDialogAnimation
import app.revanced.util.fingerprint.LiteralValueFingerprint

object ShowDialogCommandFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { SlidingDialogAnimation }
)