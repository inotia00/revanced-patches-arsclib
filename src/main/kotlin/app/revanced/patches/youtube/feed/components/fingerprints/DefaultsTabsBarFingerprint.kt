package app.revanced.patches.youtube.feed.components.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.TabsBarTextTabView
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object DefaultsTabsBarFingerprint : LiteralValueFingerprint(
    returnType = "Landroid/view/View;",
    parameters = listOf("Ljava/lang/CharSequence;", "Ljava/lang/CharSequence;", "Z"),
    literalSupplier = { TabsBarTextTabView }
)