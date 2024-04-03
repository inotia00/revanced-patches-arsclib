package app.revanced.patches.youtube.utils.overridespeed.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.AccessFlags

internal object PlaybackSpeedPatchFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("F"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "$VIDEO_PATH/PlaybackSpeedPatch;"
                && methodDef.name == "overrideSpeed"
    }
)