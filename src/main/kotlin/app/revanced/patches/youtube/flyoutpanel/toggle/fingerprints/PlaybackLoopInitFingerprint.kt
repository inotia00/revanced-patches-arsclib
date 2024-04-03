package app.revanced.patches.youtube.flyoutpanel.toggle.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Resolves with the class found in [PlaybackLoopOnClickListenerFingerprint].
 */
internal object PlaybackLoopInitFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("menu_item_single_video_playback_loop")
)