package app.revanced.patches.reddit.layout.sidebar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

internal object SidebarComponentsPatchFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/SidebarComponentsPatch;") &&
                methodDef.name == "getHeaderItemName"
    }
)