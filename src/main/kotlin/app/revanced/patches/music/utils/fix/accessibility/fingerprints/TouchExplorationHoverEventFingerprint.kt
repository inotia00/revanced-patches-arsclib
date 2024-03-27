package app.revanced.patches.music.utils.fix.accessibility.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object TouchExplorationHoverEventFingerprint : MethodFingerprint(
    returnType = "Z",
    customFingerprint = { methodDef, _ -> methodDef.name == "onTouchExplorationHoverEvent" }
)