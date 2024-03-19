package app.revanced.patches.youtube.utils.litho.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import com.android.tools.smali.dexlib2.AccessFlags

object LithoFilterFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "$COMPONENTS_PATH/LithoFilterPatch;"
    }
)