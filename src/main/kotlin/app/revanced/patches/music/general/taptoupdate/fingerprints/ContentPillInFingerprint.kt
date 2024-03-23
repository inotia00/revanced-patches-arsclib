package app.revanced.patches.music.general.taptoupdate.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object ContentPillInFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Content pill VE is null")
)