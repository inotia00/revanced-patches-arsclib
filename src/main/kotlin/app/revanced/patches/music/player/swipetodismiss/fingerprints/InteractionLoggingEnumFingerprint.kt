package app.revanced.patches.music.player.swipetodismiss.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object InteractionLoggingEnumFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("INTERACTION_LOGGING_GESTURE_TYPE_SWIPE")
)
