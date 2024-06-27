package app.revanced.patches.reddit.layout.navigation.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenFingerprint.indexOfGetDimensionPixelSize
import app.revanced.util.getTargetIndexWithMethodReferenceName
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.iface.Method

internal object BottomNavScreenFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lcom/reddit/launch/bottomnav/BottomNavScreen;"
                && indexOfGetDimensionPixelSize(methodDef) >= 0
    }
) {
    fun indexOfGetDimensionPixelSize(methodDef: Method) =
        methodDef.getTargetIndexWithMethodReferenceName("getDimensionPixelSize")
}