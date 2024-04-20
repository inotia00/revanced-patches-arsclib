package app.revanced.patches.youtube.utils.browseid.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object MobileTopBarDialogFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, classDef ->
        methodDef.name == "setContentView" &&
                classDef.superclass == "Landroid/app/Dialog;"
    }
)