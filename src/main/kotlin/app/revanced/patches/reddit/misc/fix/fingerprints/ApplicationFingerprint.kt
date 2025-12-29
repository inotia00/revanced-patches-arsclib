package app.revanced.patches.reddit.misc.fix.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

internal object ApplicationFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/content/Context;"),
    customFingerprint = { methodDef, classDef ->
        classDef.superclass == "Landroid/app/Application;" &&
                methodDef.name == "attachBaseContext"
    }
)