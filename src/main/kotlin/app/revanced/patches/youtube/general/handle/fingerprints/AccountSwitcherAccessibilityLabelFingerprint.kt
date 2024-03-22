package app.revanced.patches.youtube.general.handle.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AccountSwitcherAccessibility
import app.revanced.util.fingerprint.LiteralValueFingerprint

object AccountSwitcherAccessibilityLabelFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    literalSupplier = { AccountSwitcherAccessibility }
)