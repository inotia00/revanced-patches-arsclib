package app.revanced.patches.shared.ads.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object VideoAdsFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("markFillRequested", "requestEnterSlot")
)