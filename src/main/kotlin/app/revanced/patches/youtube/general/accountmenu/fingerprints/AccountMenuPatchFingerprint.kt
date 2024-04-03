package app.revanced.patches.youtube.general.accountmenu.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import com.android.tools.smali.dexlib2.AccessFlags

internal object AccountMenuPatchFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == GENERAL_CLASS_DESCRIPTOR
                && methodDef.name == "hideAccountMenu"
    }
)