package app.revanced.patches.youtube.utils.overridequality.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.AccessFlags

internal object VideoQualityPatchFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("I"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "$VIDEO_PATH/VideoQualityPatch;"
                && methodDef.name == "overrideQuality"
    }
)