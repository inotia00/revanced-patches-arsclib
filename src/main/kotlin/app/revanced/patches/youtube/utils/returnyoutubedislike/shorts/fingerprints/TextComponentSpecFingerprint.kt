package app.revanced.patches.youtube.utils.returnyoutubedislike.shorts.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object TextComponentSpecFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/CharSequence;",
    strings = listOf("Failed to set PB Style Run Extension in TextComponentSpec. Extension id: %s")
)