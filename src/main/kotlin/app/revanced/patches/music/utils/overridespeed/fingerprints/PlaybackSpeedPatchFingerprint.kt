package app.revanced.patches.music.utils.overridespeed.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.AccessFlags

internal object PlaybackSpeedPatchFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = emptyList(),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "$VIDEO_PATH/PlaybackSpeedPatch;"
                && methodDef.name == "showPlaybackSpeedMenu"
    }
)