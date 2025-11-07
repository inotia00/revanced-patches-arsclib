package app.revanced.patches.reddit.layout.sidebar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.layout.sidebar.SidebarUtils.indexOfHeaderItemInstruction
import org.jf.dexlib2.AccessFlags

internal object RedditProLoaderFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/Object;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ ->
        methodDef.parameterTypes.firstOrNull() == "Ljava/lang/Object;" &&
                indexOfHeaderItemInstruction(methodDef, "REDDIT_PRO") >= 0
    }
)