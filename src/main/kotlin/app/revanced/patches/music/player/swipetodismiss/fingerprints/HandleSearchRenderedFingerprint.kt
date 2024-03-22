package app.revanced.patches.music.player.swipetodismiss.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object HandleSearchRenderedFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    customFingerprint = { methodDef, _ -> methodDef.name == "handleSearchRendered" }
)
