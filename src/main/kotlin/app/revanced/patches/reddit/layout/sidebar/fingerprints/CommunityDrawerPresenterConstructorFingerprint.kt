package app.revanced.patches.reddit.layout.sidebar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.reddit.layout.sidebar.SidebarUtils.indexOfHeaderItemInstruction
import org.jf.dexlib2.AccessFlags

internal object CommunityDrawerPresenterConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    strings = listOf("communityDrawerSettings"),
    customFingerprint = { methodDef, _ ->
        indexOfHeaderItemInstruction(methodDef) >= 0
    }
)