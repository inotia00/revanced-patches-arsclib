package app.revanced.patches.reddit.misc.openlink.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.misc.openlink.fingerprints.CustomReportsFingerprint.indexOfScreenNavigator
import app.revanced.util.getTargetIndexWithReference
import org.jf.dexlib2.iface.Method

internal object CustomReportsFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("https://www.crisistextline.org/", "screenNavigator"),
    customFingerprint = { methodDef, _ ->
        indexOfScreenNavigator(methodDef) >= 0
    }
) {
    fun indexOfScreenNavigator(methodDef: Method) =
        methodDef.getTargetIndexWithReference("Landroid/app/Activity;Landroid/net/Uri;")
}