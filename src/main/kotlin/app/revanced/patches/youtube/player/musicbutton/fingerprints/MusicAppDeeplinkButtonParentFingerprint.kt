package app.revanced.patches.youtube.player.musicbutton.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.MusicAppDeeplinkButtonView
import app.revanced.util.fingerprint.LiteralValueFingerprint

object MusicAppDeeplinkButtonParentFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { MusicAppDeeplinkButtonView }
)