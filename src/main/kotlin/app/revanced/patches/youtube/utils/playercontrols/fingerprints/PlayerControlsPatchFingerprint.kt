package app.revanced.patches.youtube.utils.playercontrols.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.AccessFlags

internal object PlayerControlsPatchFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    parameters = listOf("Z", "Z"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "$UTILS_PATH/PlayerControlsPatch;"
                && methodDef.name == "changeVisibility"
    }
)