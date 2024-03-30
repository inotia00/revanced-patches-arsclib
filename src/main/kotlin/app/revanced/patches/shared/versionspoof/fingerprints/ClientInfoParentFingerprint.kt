package app.revanced.patches.shared.versionspoof.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object ClientInfoParentFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Android Wear")
)
