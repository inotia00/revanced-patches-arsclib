package app.revanced.patches.youtube.swipe.controls.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import com.android.tools.smali.dexlib2.AccessFlags

internal object SwipeControlsHostActivityFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = emptyList(),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "$INTEGRATIONS_PATH/swipecontrols/SwipeControlsHostActivity;"
    }
)
