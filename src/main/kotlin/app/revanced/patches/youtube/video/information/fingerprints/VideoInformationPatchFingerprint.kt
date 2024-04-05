package app.revanced.patches.youtube.video.information.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.AccessFlags

internal object VideoInformationPatchFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("F"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "$VIDEO_PATH/VideoInformation;"
                && methodDef.name == "overridePlaybackSpeed"
    }
)