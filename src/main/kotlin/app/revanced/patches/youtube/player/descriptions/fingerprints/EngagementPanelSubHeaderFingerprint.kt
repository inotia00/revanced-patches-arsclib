package app.revanced.patches.youtube.player.descriptions.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.PanelSubHeader
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object EngagementPanelSubHeaderFingerprint : LiteralValueFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    returnType = "V",
    parameters = listOf("Landroid/view/ViewGroup;", "L"),
    literalSupplier = { PanelSubHeader }
)