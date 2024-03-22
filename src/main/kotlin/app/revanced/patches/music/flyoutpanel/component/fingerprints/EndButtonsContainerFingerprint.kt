package app.revanced.patches.music.flyoutpanel.component.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.EndButtonsContainer
import app.revanced.util.fingerprint.LiteralValueFingerprint

object EndButtonsContainerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { EndButtonsContainer }
)

